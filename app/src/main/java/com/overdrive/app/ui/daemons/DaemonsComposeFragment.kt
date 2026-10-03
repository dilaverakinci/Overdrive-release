package com.overdrive.app.ui.daemons

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.switchmaterial.SwitchMaterial
import com.overdrive.app.BuildConfig
import com.overdrive.app.R
import com.overdrive.app.config.CloudflaredPaidConfig
import com.overdrive.app.config.UnifiedConfigManager
import com.overdrive.app.launcher.AdbDaemonLauncher
import com.overdrive.app.logging.LogUploader
import com.overdrive.app.server.DaemonIpcClient
import com.overdrive.app.ui.adapter.DaemonAdapter
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.daemon.DaemonCallback
import com.overdrive.app.ui.model.DaemonStatus
import com.overdrive.app.ui.model.DaemonType
import com.overdrive.app.ui.model.localizedName
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.ui.util.QrCodeGenerator
import com.overdrive.app.ui.viewmodel.DaemonsViewModel
import com.overdrive.app.util.DaemonHttpClient
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Modern 100% Jetpack Compose Native implementation of DaemonsFragment.
 * Manages background daemons, tunnel configs, log downloads, and Wi-Fi auto-enable.
 */
class DaemonsComposeFragment : Fragment() {

    private val handler = Handler(Looper.getMainLooper())
    private val wifiSettingsWorker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "WifiAutoEnableSettings").apply { isDaemon = true }
    }

    private val daemonsViewModel: DaemonsViewModel by activityViewModels()

    private var uiState by mutableStateOf(DaemonsUiState())
    private var applyingWifiAutoEnable = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return OverdriveComposeContainer(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OverdriveTheme {
                    DaemonsScreen(
                        state = uiState,
                        onToggleDaemon = { type, enabled -> onDaemonToggled(type, enabled) },
                        onConfigureDaemon = { type -> onDaemonConfigureClicked(type) },
                        onDownloadLog = if (BuildConfig.DEBUG || BuildConfig.LOG_CAPTURE) {
                            { type -> onDownloadLogClicked(type) }
                        } else null,
                        onToggleWifiAutoEnable = { enabled ->
                            if (!applyingWifiAutoEnable && !uiState.isWifiAutoEnableLoading) {
                                persistWifiAutoEnable(enabled)
                            }
                        },
                        onEnableBydAdbClick = {
                            uiState = uiState.copy(isBydAdbActivating = true)
                            com.overdrive.app.byd.adb.BydAdbManager.enableWirelessAdbAsync(requireContext()) { success, msg ->
                                handler.post {
                                    uiState = uiState.copy(
                                        isBydAdbActive = success,
                                        isBydAdbActivating = false,
                                        bydAdbPort = com.overdrive.app.byd.adb.BydAdbManager.lastActivePort
                                    )
                                    Toast.makeText(
                                        requireContext(),
                                        (if (success) "✓ " else "⚠ ") + msg,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        checkZrokTokenStatus()
        refreshBydAdbStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshWifiAutoEnable()
        refreshBydAdbStatus()
    }

    private fun refreshBydAdbStatus() {
        wifiSettingsWorker.execute {
            val listening = com.overdrive.app.byd.adb.BydAdbManager.isAdbListening()
            handler.post {
                uiState = uiState.copy(
                    isBydAdbActive = listening,
                    bydAdbPort = com.overdrive.app.byd.adb.BydAdbManager.lastActivePort
                )
            }
        }
    }

    override fun onDestroy() {
        wifiSettingsWorker.shutdown()
        super.onDestroy()
    }

    private fun observeViewModel() {
        daemonsViewModel.daemonStates.observe(viewLifecycleOwner) { states ->
            val sortedList = states.values.sortedBy { it.type.ordinal }
            uiState = uiState.copy(daemons = sortedList)
        }
    }

    private fun refreshWifiAutoEnable() {
        uiState = uiState.copy(isWifiAutoEnableLoading = true)
        wifiSettingsWorker.execute {
            val enabled = runCatching {
                UnifiedConfigManager.forceReload()
                UnifiedConfigManager.isWifiAutoEnableEnabled()
            }.getOrDefault(true)
            handler.post {
                if (view == null) return@post
                applyingWifiAutoEnable = true
                uiState = uiState.copy(
                    isWifiAutoEnable = enabled,
                    isWifiAutoEnableLoading = false
                )
                applyingWifiAutoEnable = false
            }
        }
    }

    private fun persistWifiAutoEnable(enabled: Boolean) {
        uiState = uiState.copy(isWifiAutoEnableLoading = true)
        wifiSettingsWorker.execute {
            val applied = setWifiEnabledThroughDaemon(enabled)
            val persistedState = runCatching {
                UnifiedConfigManager.forceReload()
                UnifiedConfigManager.isWifiAutoEnableEnabled()
            }.getOrDefault(!enabled)
            handler.post {
                if (view == null) return@post
                applyingWifiAutoEnable = true
                uiState = uiState.copy(
                    isWifiAutoEnable = persistedState,
                    isWifiAutoEnableLoading = false
                )
                applyingWifiAutoEnable = false
                if (!applied || persistedState != enabled) {
                    context?.let {
                        Toast.makeText(
                            it,
                            R.string.daemons_wifi_auto_enable_save_failed,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun setWifiEnabledThroughDaemon(enabled: Boolean): Boolean {
        var connection: java.net.HttpURLConnection? = null
        return try {
            connection = DaemonHttpClient.open("/api/keymap/fire", "POST", 2000, 7000)
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            val payload = JSONObject()
                .put("kind", "radio")
                .put("radio", "wifi")
                .put("enable", enabled)
            connection.outputStream.use {
                it.write(payload.toString().toByteArray(Charsets.UTF_8))
            }
            if (connection.responseCode !in 200..299) return false
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                .optBoolean("success", false)
        } catch (_: Throwable) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun checkZrokTokenStatus() {
        daemonsViewModel.zrokController.hasEnableToken { hasToken ->
            activity?.runOnUiThread {
                if (!hasToken) {
                    daemonsViewModel.updateZrokNeedsConfig(getString(R.string.daemon_config_no_token))
                }
            }
        }
    }

    private fun onDaemonToggled(type: DaemonType, enabled: Boolean) {
        daemonsViewModel.daemonStartupManager?.onDaemonToggled(type, enabled)
        if (enabled) {
            daemonsViewModel.startDaemon(type)
        } else {
            daemonsViewModel.stopDaemon(type)
        }
    }

    private fun onDaemonConfigureClicked(type: DaemonType) {
        DaemonDialogManager.showConfigDialog(type, this, daemonsViewModel)
    }

    private fun showZrokTokenDialog() {
        val context = context ?: return
        daemonsViewModel.zrokController.getEnableToken { currentToken ->
            activity?.runOnUiThread {
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_zrok_token, null)
                val editToken = dialogView.findViewById<EditText>(R.id.editZrokToken)
                currentToken?.let { editToken.setText(it) }

                val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(context, R.style.Theme_Overdrive_M3_Dialog)
                    .setIcon(R.drawable.ic_link)
                    .setTitle(getString(R.string.dialog_zrok_token_title))
                    .setView(dialogView)
                    .setPositiveButton(getString(R.string.dialog_save)) { _, _ ->
                        val token = editToken.text.toString().trim()
                        if (token.isNotEmpty()) {
                            saveZrokToken(token)
                        } else {
                            Toast.makeText(context, getString(R.string.toast_token_cannot_be_empty), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton(getString(R.string.action_cancel), null)
                    .setNeutralButton(getString(R.string.dialog_delete)) { _, _ ->
                        deleteZrokToken()
                    }
                    .create()

                dialogView.findViewById<View>(R.id.btnResetZrokEnvironment)?.setOnClickListener {
                    dialog.dismiss()
                    confirmResetZrokEnvironment()
                }

                dialog.show()
            }
        }
    }

    private fun showTailscaleSettingsDialog() {
        val context = context ?: return
        var loginGenerated = false

        activity?.runOnUiThread {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_tailscale_settings, null)
            val loginGenerateButton = dialogView.findViewById<TextView>(R.id.generateLoginUrlBtn)
            val qrCodeContainer = dialogView.findViewById<LinearLayout>(R.id.qrCodeContainer)
            val qrCodeText = dialogView.findViewById<TextView>(R.id.qrCodeURL)
            val qrCodeImage = dialogView.findViewById<ImageView>(R.id.qrCodeImage)
            val proxySwitch = dialogView.findViewById<SwitchMaterial>(R.id.switchTailscaleProxy)
            val adbSwitch = dialogView.findViewById<SwitchMaterial>(R.id.switchTailscaleAdb)
            val httpsSwitch = dialogView.findViewById<SwitchMaterial>(R.id.switchTailscaleHttps)
            val adbEndpoint = dialogView.findViewById<TextView>(R.id.tailscaleAdbEndpoint)

            daemonsViewModel.tailscaleController.isProxyEnabled { isEnabled ->
                activity?.runOnUiThread {
                    proxySwitch.isChecked = isEnabled
                }
            }

            daemonsViewModel.tailscaleController.isAdbEnabled { isEnabled ->
                activity?.runOnUiThread {
                    adbSwitch.isChecked = isEnabled
                }
            }

            daemonsViewModel.tailscaleController.isHttpsEnabled { isEnabled ->
                activity?.runOnUiThread {
                    httpsSwitch.isChecked = isEnabled
                }
            }

            daemonsViewModel.tailscaleController.getAdbEndpoint { endpoint ->
                activity?.runOnUiThread {
                    val ctx = context ?: return@runOnUiThread
                    if (endpoint.isNullOrEmpty()) {
                        adbEndpoint.visibility = View.GONE
                    } else {
                        val command = ctx.getString(R.string.tailscale_adb_endpoint, endpoint)
                        adbEndpoint.text = command
                        adbEndpoint.visibility = View.VISIBLE
                        adbEndpoint.setOnClickListener {
                            val tapCtx = context ?: return@setOnClickListener
                            val clip = tapCtx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                    as? android.content.ClipboardManager
                            clip?.setPrimaryClip(android.content.ClipData.newPlainText("adb", command))
                            Toast.makeText(tapCtx, tapCtx.getString(R.string.tailscale_adb_endpoint_copied), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            loginGenerateButton.setOnClickListener {
                if (!loginGenerated) {
                    loginGenerated = true
                    qrCodeContainer.visibility = View.VISIBLE
                    daemonsViewModel.tailscaleController.generateLoginUrl { url ->
                        activity?.runOnUiThread {
                            if (url != null) {
                                val qrBitmap = QrCodeGenerator.generate(url, 400)
                                qrCodeImage.setImageBitmap(qrBitmap)
                                qrCodeText.text = url
                                qrCodeText.setTextColor(ContextCompat.getColor(context, R.color.brand_primary))
                            } else {
                                qrCodeText.text = getString(R.string.tailscale_failed_login_url)
                                qrCodeText.setTextColor(ContextCompat.getColor(context, R.color.status_danger))
                                loginGenerated = false
                            }
                        }
                    }
                }
            }

            daemonsViewModel.tailscaleController.tunnelUrl.observe(viewLifecycleOwner) { url ->
                if (loginGenerated && !url.isNullOrEmpty()) {
                    activity?.runOnUiThread {
                        qrCodeContainer.visibility = View.GONE
                        loginGenerated = false
                        loginGenerateButton.text = getString(R.string.tailscale_logged_in_relogin)
                    }
                }
            }

            val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(context, R.style.Theme_Overdrive_M3_Dialog)
                .setIcon(R.drawable.ic_mqtt)
                .setTitle(getString(R.string.dialog_tailscale_settings_title))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.dialog_save)) { _, _ ->
                    val enableProxy = proxySwitch.isChecked
                    val enableAdb = adbSwitch.isChecked
                    val enableHttps = httpsSwitch.isChecked

                    daemonsViewModel.tailscaleController.isHttpsEnabled { httpsWasEnabled ->
                        activity?.runOnUiThread {
                            if (enableHttps != httpsWasEnabled) {
                                saveTailscaleHttpsSettings(enableHttps)
                            }
                        }
                    }

                    val thenProxy = {
                        daemonsViewModel.tailscaleController.isProxyEnabled { wasEnabled ->
                            activity?.runOnUiThread {
                                if (enableProxy && !wasEnabled) {
                                    confirmEnableTailscaleProxy()
                                } else if (enableProxy != wasEnabled) {
                                    saveTailscaleProxySettings(enableProxy)
                                }
                            }
                        }
                    }
                    daemonsViewModel.tailscaleController.isAdbEnabled { adbWasEnabled ->
                        activity?.runOnUiThread {
                            if (enableAdb != adbWasEnabled) {
                                if (enableAdb) confirmEnableTailscaleAdb(thenProxy)
                                else {
                                    saveTailscaleAdbSettings(false)
                                    thenProxy()
                                }
                            } else {
                                thenProxy()
                            }
                        }
                    }
                }
                .setNegativeButton(getString(R.string.action_cancel), null)
                .setNeutralButton(getString(R.string.dialog_delete)) { _, _ ->
                    confirmResetTailscaleEnvironment()
                }
                .create()

            dialog.show()
        }
    }

    private fun confirmEnableTailscaleAdb(onDismissed: (() -> Unit)? = null) {
        val context = context ?: run {
            onDismissed?.invoke()
            return
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(context, R.style.Theme_Overdrive_M3_Dialog)
            .setIcon(R.drawable.ic_warning)
            .setTitle(getString(R.string.dialog_tailscale_adb_enable_title))
            .setMessage(getString(R.string.dialog_tailscale_adb_enable_message))
            .setPositiveButton(getString(R.string.dialog_enable)) { _, _ ->
                saveTailscaleAdbSettings(true)
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .setOnDismissListener { onDismissed?.invoke() }
            .show()
    }

    private fun saveTailscaleAdbSettings(enabled: Boolean) {
        daemonsViewModel.tailscaleController.saveAdbSettings(enabled) { saved ->
            activity?.runOnUiThread {
                val ctx = context ?: return@runOnUiThread
                if (!saved) {
                    Toast.makeText(ctx, getString(R.string.toast_tailscale_adb_save_failed), Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                val msg = if (enabled) R.string.toast_tailscale_adb_enabled
                else R.string.toast_tailscale_adb_disabled
                Toast.makeText(ctx, getString(msg), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveTailscaleHttpsSettings(enabled: Boolean) {
        daemonsViewModel.tailscaleController.saveHttpsSettings(enabled) { saved ->
            activity?.runOnUiThread {
                val ctx = context ?: return@runOnUiThread
                if (!saved) {
                    Toast.makeText(
                        ctx,
                        getString(R.string.toast_tailscale_https_save_failed),
                        Toast.LENGTH_LONG
                    ).show()
                    return@runOnUiThread
                }
                val message = if (enabled) {
                    R.string.toast_tailscale_https_enabled
                } else {
                    R.string.toast_tailscale_https_disabled
                }
                Toast.makeText(ctx, getString(message), Toast.LENGTH_SHORT).show()
                daemonsViewModel.tailscaleController.refreshTunnelUrl()
            }
        }
    }

    private fun confirmEnableTailscaleProxy() {
        val context = context ?: return

        com.google.android.material.dialog.MaterialAlertDialogBuilder(context, R.style.Theme_Overdrive_M3_Dialog)
            .setIcon(R.drawable.ic_warning)
            .setTitle(getString(R.string.dialog_tailscale_proxy_enable_title))
            .setMessage(getString(R.string.dialog_tailscale_proxy_enable_message))
            .setPositiveButton(getString(R.string.dialog_enable)) { _, _ ->
                saveTailscaleProxySettings(true)
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun confirmResetZrokEnvironment() {
        val context = context ?: return

        com.google.android.material.dialog.MaterialAlertDialogBuilder(context, R.style.Theme_Overdrive_M3_Dialog)
            .setIcon(R.drawable.ic_warning)
            .setTitle(getString(R.string.dialog_zrok_reset_title))
            .setMessage(getString(R.string.dialog_zrok_reset_message))
            .setPositiveButton(getString(R.string.dialog_reset)) { _, _ ->
                resetZrokEnvironment()
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun resetZrokEnvironment() {
        val context = context ?: return
        Toast.makeText(context, getString(R.string.toast_resetting_zrok), Toast.LENGTH_SHORT).show()

        daemonsViewModel.stopDaemon(DaemonType.ZROK_TUNNEL)

        daemonsViewModel.zrokController.disableEnvironment(object : DaemonCallback {
            override fun onStatusChanged(status: DaemonStatus, message: String) {
                daemonsViewModel.zrokController.deleteEnableToken { success ->
                    activity?.runOnUiThread {
                        if (success) {
                            Toast.makeText(context, getString(R.string.toast_zrok_reset_success), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, getString(R.string.toast_zrok_reset_partial), Toast.LENGTH_LONG).show()
                        }
                        daemonsViewModel.updateZrokNeedsConfig(getString(R.string.zrok_no_token_configured))
                    }
                }
            }

            override fun onError(error: String) {
                daemonsViewModel.zrokController.deleteEnableToken { _ ->
                    activity?.runOnUiThread {
                        Toast.makeText(context, getString(R.string.toast_zrok_reset_warnings, error), Toast.LENGTH_LONG).show()
                        daemonsViewModel.updateZrokNeedsConfig(getString(R.string.zrok_no_token_configured))
                    }
                }
            }
        })
    }

    private fun confirmResetTailscaleEnvironment() {
        val context = context ?: return

        com.google.android.material.dialog.MaterialAlertDialogBuilder(context, R.style.Theme_Overdrive_M3_Dialog)
            .setIcon(R.drawable.ic_warning)
            .setTitle(getString(R.string.dialog_tailscale_reset_title))
            .setMessage(getString(R.string.dialog_tailscale_reset_message))
            .setPositiveButton(getString(R.string.dialog_reset)) { _, _ ->
                resetTailscaleEnvironment()
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun resetTailscaleEnvironment() {
        val context = context ?: return
        Toast.makeText(context, getString(R.string.toast_resetting_tailscale), Toast.LENGTH_SHORT).show()

        daemonsViewModel.stopDaemon(DaemonType.TAILSCALE_TUNNEL)

        daemonsViewModel.tailscaleController.disableEnvironment(object : DaemonCallback {
            override fun onStatusChanged(status: DaemonStatus, message: String) {
                Toast.makeText(context, getString(R.string.toast_tailscale_reset_success), Toast.LENGTH_LONG).show()
            }

            override fun onError(error: String) {
                Toast.makeText(context, getString(R.string.toast_tailscale_reset_warnings, error), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun saveTailscaleProxySettings(enabled: Boolean) {
        daemonsViewModel.tailscaleController.saveProxySettings(enabled) { saved ->
            activity?.runOnUiThread {
                if (saved != null) {
                    if (saved) {
                        com.overdrive.app.mqtt.ProxyHelper.invalidateCache()

                        val status = daemonsViewModel.daemonStates.value?.get(DaemonType.TAILSCALE_TUNNEL)?.status
                        if (status != DaemonStatus.STOPPED) {
                            daemonsViewModel.stopDaemon(DaemonType.TAILSCALE_TUNNEL)
                            handler.postDelayed(
                                { daemonsViewModel.startDaemon(DaemonType.TAILSCALE_TUNNEL) },
                                2000
                            )
                        }
                        if (enabled) {
                            Toast.makeText(context, getString(R.string.toast_tailscale_proxy_enabled), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, getString(R.string.toast_tailscale_proxy_disabled), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, getString(R.string.toast_tailscale_proxy_save_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun saveZrokToken(token: String) {
        daemonsViewModel.zrokController.saveEnableToken(token) { success ->
            activity?.runOnUiThread {
                if (success) {
                    Toast.makeText(context, getString(R.string.toast_zrok_token_saved), Toast.LENGTH_SHORT).show()
                    daemonsViewModel.refreshDaemonStatus(DaemonType.ZROK_TUNNEL)
                } else {
                    Toast.makeText(context, getString(R.string.toast_zrok_token_save_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteZrokToken() {
        daemonsViewModel.zrokController.deleteEnableToken { success ->
            activity?.runOnUiThread {
                if (success) {
                    Toast.makeText(context, getString(R.string.toast_zrok_token_deleted), Toast.LENGTH_SHORT).show()
                    daemonsViewModel.updateZrokNeedsConfig(getString(R.string.zrok_no_token_configured))
                } else {
                    Toast.makeText(context, getString(R.string.toast_zrok_token_delete_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onDownloadLogClicked(type: DaemonType) {
        DaemonDialogManager.downloadLog(type, this, daemonsViewModel)
    }

    private fun uploadDaemonLog(type: DaemonType, localizedName: String) {
        val ctx = context ?: return
        val daemonKey = DaemonAdapter.daemonLogKey(type) ?: run {
            Toast.makeText(ctx, getString(R.string.toast_log_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        val progressView = LayoutInflater.from(ctx).inflate(R.layout.dialog_log_uploading, null)
        val progressDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
            ctx, R.style.Theme_Overdrive_M3_Dialog)
            .setView(progressView)
            .setCancelable(false)
            .show()

        Thread {
            val req = JSONObject().apply {
                put("command", "UPLOAD_LOG")
                put("daemon", daemonKey)
            }
            val resp = DaemonIpcClient.send(req, 35_000)
            activity?.runOnUiThread {
                progressDialog.dismiss()
                if (!isAdded) return@runOnUiThread
                val ctx2 = context ?: return@runOnUiThread
                if (resp == null || !resp.optBoolean("success", false)) {
                    val err = resp?.optString("error") ?: getString(R.string.errors_network)
                    Toast.makeText(ctx2, getString(R.string.toast_log_save_failed, err), Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                val code = resp.optString("code", "")
                showLogUploadedDialog(ctx2, code)
            }
        }.start()
    }

    private fun showLogUploadedDialog(ctx: android.content.Context, code: String) {
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_log_uploaded, null)
        view.findViewById<TextView>(R.id.uploadedCode)?.text = code

        fun copyCode() {
            val clip = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                    as? android.content.ClipboardManager
            clip?.setPrimaryClip(android.content.ClipData.newPlainText("log code", code))
            Toast.makeText(ctx, R.string.toast_url_copied_short, Toast.LENGTH_SHORT).show()
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            ctx, R.style.Theme_Overdrive_M3_Dialog)
            .setView(view)
            .setPositiveButton(R.string.logs_copy_code) { _, _ -> copyCode() }
            .setNegativeButton(R.string.logs_done, null)
            .show()

        view.findViewById<View>(R.id.codeCard)?.setOnClickListener { copyCode() }
    }

    private fun shareDaemonLog(type: DaemonType) {
        val logPath = DaemonAdapter.getLogFilePath(type) ?: return
        val ctx = context ?: return
        val daemonName = type.displayName.replace(" ", "_").lowercase(Locale.ROOT)
        val localizedName = type.localizedName(ctx)

        val adb = daemonsViewModel.daemonStartupManager?.adbLauncher
        if (adb == null) {
            Toast.makeText(ctx, getString(R.string.toast_daemon_manager_unavailable), Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(ctx, getString(R.string.toast_fetching_log, localizedName), Toast.LENGTH_SHORT).show()

        adb.executeShellCommand(
            "wc -l < $logPath 2>/dev/null; echo '---SEPARATOR---'; tail -10000 $logPath 2>/dev/null",
            object : AdbDaemonLauncher.LaunchCallback {
                override fun onLog(message: String) {
                    activity?.runOnUiThread {
                        if (message.isBlank()) {
                            Toast.makeText(ctx, getString(R.string.toast_log_empty_or_missing), Toast.LENGTH_SHORT).show()
                            return@runOnUiThread
                        }

                        try {
                            val parts = message.split("---SEPARATOR---", limit = 2)
                            val totalLines = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
                            val logContent = parts.getOrNull(1)?.trimStart('\n') ?: message

                            if (logContent.isBlank()) {
                                Toast.makeText(ctx, getString(R.string.toast_log_empty), Toast.LENGTH_SHORT).show()
                                return@runOnUiThread
                            }

                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val fileName = "${daemonName}_${timestamp}.log"
                            val cacheDir = File(ctx.cacheDir, "logs")
                            cacheDir.mkdirs()
                            val logFile = File(cacheDir, fileName)

                            val header = buildString {
                                appendLine(getString(R.string.log_header_title, localizedName))
                                appendLine(getString(R.string.log_header_source, logPath))
                                appendLine(getString(R.string.log_header_exported, Date().toString()))
                                if (totalLines > 10000) {
                                    appendLine(getString(R.string.log_header_truncated, totalLines))
                                }
                                appendLine("===")
                                appendLine()
                            }
                            logFile.writeText(header + logContent)

                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                ctx,
                                "${ctx.packageName}.fileprovider",
                                logFile
                            )

                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                this.type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.log_share_title, localizedName, timestamp))
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                clipData = android.content.ClipData.newRawUri(null, uri)
                            }
                            startActivity(android.content.Intent.createChooser(shareIntent, getString(R.string.log_share_chooser, localizedName)))
                        } catch (e: Exception) {
                            Toast.makeText(ctx, getString(R.string.toast_log_save_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onLaunched() {}

                override fun onError(error: String) {
                    activity?.runOnUiThread {
                        Toast.makeText(ctx, getString(R.string.toast_log_not_found), Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}
