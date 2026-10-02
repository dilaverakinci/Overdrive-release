package com.overdrive.app.ui.daemons

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.overdrive.app.R
import com.overdrive.app.config.CloudflaredPaidConfig
import com.overdrive.app.launcher.AdbDaemonLauncher
import com.overdrive.app.logging.LogUploader
import com.overdrive.app.server.DaemonIpcClient
import com.overdrive.app.ui.adapter.DaemonAdapter
import com.overdrive.app.ui.daemon.DaemonCallback
import com.overdrive.app.ui.model.DaemonStatus
import com.overdrive.app.ui.model.DaemonType
import com.overdrive.app.ui.model.localizedName
import com.overdrive.app.ui.util.QrCodeGenerator
import com.overdrive.app.ui.viewmodel.DaemonsViewModel
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared manager for daemon configuration dialogs (Zrok, Tailscale, Cloudflare)
 * and log export/download actions across both DaemonsComposeFragment and SettingsComposeFragment.
 */
object DaemonDialogManager {

    private val handler = Handler(Looper.getMainLooper())

    fun showConfigDialog(
        type: DaemonType,
        fragment: Fragment,
        daemonsViewModel: DaemonsViewModel
    ) {
        val context = fragment.context ?: return
        when (type) {
            DaemonType.ZROK_TUNNEL -> showZrokTokenDialog(fragment, daemonsViewModel)
            DaemonType.TAILSCALE_TUNNEL -> showTailscaleSettingsDialog(fragment, daemonsViewModel)
            DaemonType.CLOUDFLARED_TUNNEL -> {
                CloudflaredPaidConfig.showSettingsDialog(context, daemonsViewModel)
            }
            else -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_no_config_needed, type.localizedName(context)),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =========================================================================
    // ZROK CONFIGURATION
    // =========================================================================

    private fun showZrokTokenDialog(fragment: Fragment, daemonsViewModel: DaemonsViewModel) {
        val context = fragment.context ?: return
        val activity = fragment.activity ?: return

        daemonsViewModel.zrokController.getEnableToken { currentToken ->
            activity.runOnUiThread {
                if (!fragment.isAdded) return@runOnUiThread
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_zrok_token, null)
                val editToken = dialogView.findViewById<EditText>(R.id.editZrokToken)
                currentToken?.let { editToken.setText(it) }

                val dialog = MaterialAlertDialogBuilder(context, R.style.Theme_Overdrive_M3_Dialog)
                    .setIcon(R.drawable.ic_link)
                    .setTitle(context.getString(R.string.dialog_zrok_token_title))
                    .setView(dialogView)
                    .setPositiveButton(context.getString(R.string.dialog_save)) { _, _ ->
                        val token = editToken.text.toString().trim()
                        if (token.isNotEmpty()) {
                            saveZrokToken(context, activity, daemonsViewModel, token)
                        } else {
                            Toast.makeText(context, context.getString(R.string.toast_token_cannot_be_empty), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton(context.getString(R.string.action_cancel), null)
                    .setNeutralButton(context.getString(R.string.dialog_delete)) { _, _ ->
                        deleteZrokToken(context, activity, daemonsViewModel)
                    }
                    .create()

                dialogView.findViewById<View>(R.id.btnResetZrokEnvironment)?.setOnClickListener {
                    dialog.dismiss()
                    confirmResetZrokEnvironment(context, activity, daemonsViewModel)
                }

                dialog.show()
            }
        }
    }

    private fun saveZrokToken(
        context: Context,
        activity: Activity,
        daemonsViewModel: DaemonsViewModel,
        token: String
    ) {
        daemonsViewModel.zrokController.saveEnableToken(token) { success ->
            activity.runOnUiThread {
                if (success) {
                    Toast.makeText(context, context.getString(R.string.toast_zrok_token_saved), Toast.LENGTH_SHORT).show()
                    daemonsViewModel.refreshDaemonStatus(DaemonType.ZROK_TUNNEL)
                } else {
                    Toast.makeText(context, context.getString(R.string.toast_zrok_token_save_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteZrokToken(
        context: Context,
        activity: Activity,
        daemonsViewModel: DaemonsViewModel
    ) {
        daemonsViewModel.zrokController.deleteEnableToken { success ->
            activity.runOnUiThread {
                if (success) {
                    Toast.makeText(context, context.getString(R.string.toast_zrok_token_deleted), Toast.LENGTH_SHORT).show()
                    daemonsViewModel.updateZrokNeedsConfig(context.getString(R.string.zrok_no_token_configured))
                } else {
                    Toast.makeText(context, context.getString(R.string.toast_zrok_token_delete_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun confirmResetZrokEnvironment(
        context: Context,
        activity: Activity,
        daemonsViewModel: DaemonsViewModel
    ) {
        MaterialAlertDialogBuilder(context, R.style.Theme_Overdrive_M3_Dialog)
            .setIcon(R.drawable.ic_warning)
            .setTitle(context.getString(R.string.dialog_zrok_reset_title))
            .setMessage(context.getString(R.string.dialog_zrok_reset_message))
            .setPositiveButton(context.getString(R.string.dialog_reset)) { _, _ ->
                resetZrokEnvironment(context, activity, daemonsViewModel)
            }
            .setNegativeButton(context.getString(R.string.action_cancel), null)
            .show()
    }

    private fun resetZrokEnvironment(
        context: Context,
        activity: Activity,
        daemonsViewModel: DaemonsViewModel
    ) {
        Toast.makeText(context, context.getString(R.string.toast_resetting_zrok), Toast.LENGTH_SHORT).show()
        daemonsViewModel.stopDaemon(DaemonType.ZROK_TUNNEL)

        daemonsViewModel.zrokController.disableEnvironment(object : DaemonCallback {
            override fun onStatusChanged(status: DaemonStatus, message: String) {
                daemonsViewModel.zrokController.deleteEnableToken { success ->
                    activity.runOnUiThread {
                        if (success) {
                            Toast.makeText(context, context.getString(R.string.toast_zrok_reset_success), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.toast_zrok_reset_partial), Toast.LENGTH_LONG).show()
                        }
                        daemonsViewModel.updateZrokNeedsConfig(context.getString(R.string.zrok_no_token_configured))
                    }
                }
            }

            override fun onError(error: String) {
                daemonsViewModel.zrokController.deleteEnableToken { _ ->
                    activity.runOnUiThread {
                        Toast.makeText(context, context.getString(R.string.toast_zrok_reset_warnings, error), Toast.LENGTH_LONG).show()
                        daemonsViewModel.updateZrokNeedsConfig(context.getString(R.string.zrok_no_token_configured))
                    }
                }
            }
        })
    }

    // =========================================================================
    // TAILSCALE CONFIGURATION
    // =========================================================================

    private fun showTailscaleSettingsDialog(fragment: Fragment, daemonsViewModel: DaemonsViewModel) {
        val context = fragment.context ?: return
        val activity = fragment.activity ?: return
        var loginGenerated = false

        activity.runOnUiThread {
            if (!fragment.isAdded) return@runOnUiThread
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
                activity.runOnUiThread {
                    proxySwitch.isChecked = isEnabled
                }
            }

            daemonsViewModel.tailscaleController.isAdbEnabled { isEnabled ->
                activity.runOnUiThread {
                    adbSwitch.isChecked = isEnabled
                }
            }

            daemonsViewModel.tailscaleController.isHttpsEnabled { isEnabled ->
                activity.runOnUiThread {
                    httpsSwitch.isChecked = isEnabled
                }
            }

            daemonsViewModel.tailscaleController.getAdbEndpoint { endpoint ->
                activity.runOnUiThread {
                    if (endpoint.isNullOrEmpty()) {
                        adbEndpoint.visibility = View.GONE
                    } else {
                        val command = context.getString(R.string.tailscale_adb_endpoint, endpoint)
                        adbEndpoint.text = command
                        adbEndpoint.visibility = View.VISIBLE
                        adbEndpoint.setOnClickListener {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clip?.setPrimaryClip(ClipData.newPlainText("adb", command))
                            Toast.makeText(context, context.getString(R.string.tailscale_adb_endpoint_copied), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            loginGenerateButton.setOnClickListener {
                if (!loginGenerated) {
                    loginGenerated = true
                    qrCodeContainer.visibility = View.VISIBLE
                    daemonsViewModel.tailscaleController.generateLoginUrl { url ->
                        activity.runOnUiThread {
                            if (url != null) {
                                val qrBitmap = QrCodeGenerator.generate(url, 400)
                                qrCodeImage.setImageBitmap(qrBitmap)
                                qrCodeText.text = url
                                qrCodeText.setTextColor(ContextCompat.getColor(context, R.color.brand_primary))
                            } else {
                                qrCodeText.text = context.getString(R.string.tailscale_failed_login_url)
                                qrCodeText.setTextColor(ContextCompat.getColor(context, R.color.status_danger))
                                loginGenerated = false
                            }
                        }
                    }
                }
            }

            daemonsViewModel.tailscaleController.tunnelUrl.observe(fragment.viewLifecycleOwner) { url ->
                if (loginGenerated && !url.isNullOrEmpty()) {
                    activity.runOnUiThread {
                        qrCodeContainer.visibility = View.GONE
                        loginGenerated = false
                        loginGenerateButton.text = context.getString(R.string.tailscale_logged_in_relogin)
                    }
                }
            }

            val dialog = MaterialAlertDialogBuilder(context, R.style.Theme_Overdrive_M3_Dialog)
                .setIcon(R.drawable.ic_mqtt)
                .setTitle(context.getString(R.string.dialog_tailscale_settings_title))
                .setView(dialogView)
                .setPositiveButton(context.getString(R.string.dialog_save)) { _, _ ->
                    val enableProxy = proxySwitch.isChecked
                    val enableAdb = adbSwitch.isChecked
                    val enableHttps = httpsSwitch.isChecked

                    daemonsViewModel.tailscaleController.isHttpsEnabled { httpsWasEnabled ->
                        activity.runOnUiThread {
                            if (enableHttps != httpsWasEnabled) {
                                saveTailscaleHttpsSettings(context, activity, daemonsViewModel, enableHttps)
                            }
                        }
                    }

                    val thenProxy = {
                        daemonsViewModel.tailscaleController.isProxyEnabled { wasEnabled ->
                            activity.runOnUiThread {
                                if (enableProxy && !wasEnabled) {
                                    confirmEnableTailscaleProxy(context, activity, daemonsViewModel)
                                } else if (enableProxy != wasEnabled) {
                                    saveTailscaleProxySettings(context, activity, daemonsViewModel, enableProxy)
                                }
                            }
                        }
                    }
                    daemonsViewModel.tailscaleController.isAdbEnabled { adbWasEnabled ->
                        activity.runOnUiThread {
                            if (enableAdb != adbWasEnabled) {
                                if (enableAdb) {
                                    confirmEnableTailscaleAdb(context, activity, daemonsViewModel, thenProxy)
                                } else {
                                    saveTailscaleAdbSettings(context, activity, daemonsViewModel, false)
                                    thenProxy()
                                }
                            } else {
                                thenProxy()
                            }
                        }
                    }
                }
                .setNegativeButton(context.getString(R.string.action_cancel), null)
                .setNeutralButton(context.getString(R.string.dialog_delete)) { _, _ ->
                    confirmResetTailscaleEnvironment(context, activity, daemonsViewModel)
                }
                .create()

            dialog.show()
        }
    }

    private fun confirmEnableTailscaleAdb(
        context: Context,
        activity: Activity,
        daemonsViewModel: DaemonsViewModel,
        onDismissed: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(context, R.style.Theme_Overdrive_M3_Dialog)
            .setIcon(R.drawable.ic_warning)
            .setTitle(context.getString(R.string.dialog_tailscale_adb_enable_title))
            .setMessage(context.getString(R.string.dialog_tailscale_adb_enable_message))
            .setPositiveButton(context.getString(R.string.dialog_enable)) { _, _ ->
                saveTailscaleAdbSettings(context, activity, daemonsViewModel, true)
            }
            .setNegativeButton(context.getString(R.string.action_cancel), null)
            .setOnDismissListener { onDismissed?.invoke() }
            .show()
    }

    private fun saveTailscaleAdbSettings(
        context: Context,
        activity: Activity,
        daemonsViewModel: DaemonsViewModel,
        enabled: Boolean
    ) {
        daemonsViewModel.tailscaleController.saveAdbSettings(enabled) { saved ->
            activity.runOnUiThread {
                if (!saved) {
                    Toast.makeText(context, context.getString(R.string.toast_tailscale_adb_save_failed), Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                val msg = if (enabled) R.string.toast_tailscale_adb_enabled
                else R.string.toast_tailscale_adb_disabled
                Toast.makeText(context, context.getString(msg), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveTailscaleHttpsSettings(
        context: Context,
        activity: Activity,
        daemonsViewModel: DaemonsViewModel,
        enabled: Boolean
    ) {
        daemonsViewModel.tailscaleController.saveHttpsSettings(enabled) { saved ->
            activity.runOnUiThread {
                if (!saved) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_tailscale_https_save_failed),
                        Toast.LENGTH_LONG
                    ).show()
                    return@runOnUiThread
                }
                val message = if (enabled) {
                    R.string.toast_tailscale_https_enabled
                } else {
                    R.string.toast_tailscale_https_disabled
                }
                Toast.makeText(context, context.getString(message), Toast.LENGTH_SHORT).show()
                daemonsViewModel.tailscaleController.refreshTunnelUrl()
            }
        }
    }

    private fun confirmEnableTailscaleProxy(
        context: Context,
        activity: Activity,
        daemonsViewModel: DaemonsViewModel
    ) {
        MaterialAlertDialogBuilder(context, R.style.Theme_Overdrive_M3_Dialog)
            .setIcon(R.drawable.ic_warning)
            .setTitle(context.getString(R.string.dialog_tailscale_proxy_enable_title))
            .setMessage(context.getString(R.string.dialog_tailscale_proxy_enable_message))
            .setPositiveButton(context.getString(R.string.dialog_enable)) { _, _ ->
                saveTailscaleProxySettings(context, activity, daemonsViewModel, true)
            }
            .setNegativeButton(context.getString(R.string.action_cancel)) { _, _ ->
                saveTailscaleProxySettings(context, activity, daemonsViewModel, false)
            }
            .show()
    }

    private fun saveTailscaleProxySettings(
        context: Context,
        activity: Activity,
        daemonsViewModel: DaemonsViewModel,
        enabled: Boolean
    ) {
        daemonsViewModel.tailscaleController.saveProxySettings(enabled) { saved ->
            activity.runOnUiThread {
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
                            Toast.makeText(context, context.getString(R.string.toast_tailscale_proxy_enabled), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.toast_tailscale_proxy_disabled), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.toast_tailscale_proxy_save_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun confirmResetTailscaleEnvironment(
        context: Context,
        activity: Activity,
        daemonsViewModel: DaemonsViewModel
    ) {
        MaterialAlertDialogBuilder(context, R.style.Theme_Overdrive_M3_Dialog)
            .setIcon(R.drawable.ic_warning)
            .setTitle(context.getString(R.string.dialog_tailscale_reset_title))
            .setMessage(context.getString(R.string.dialog_tailscale_reset_message))
            .setPositiveButton(context.getString(R.string.dialog_reset)) { _, _ ->
                resetTailscaleEnvironment(context, activity, daemonsViewModel)
            }
            .setNegativeButton(context.getString(R.string.action_cancel), null)
            .show()
    }

    private fun resetTailscaleEnvironment(
        context: Context,
        activity: Activity,
        daemonsViewModel: DaemonsViewModel
    ) {
        Toast.makeText(context, context.getString(R.string.toast_resetting_tailscale), Toast.LENGTH_SHORT).show()
        daemonsViewModel.stopDaemon(DaemonType.TAILSCALE_TUNNEL)

        daemonsViewModel.tailscaleController.disableEnvironment(object : DaemonCallback {
            override fun onStatusChanged(status: DaemonStatus, message: String) {
                activity.runOnUiThread {
                    Toast.makeText(context, context.getString(R.string.toast_tailscale_reset_success), Toast.LENGTH_LONG).show()
                }
            }

            override fun onError(error: String) {
                activity.runOnUiThread {
                    Toast.makeText(context, context.getString(R.string.toast_tailscale_reset_warnings, error), Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    // =========================================================================
    // LOG DOWNLOAD & SHARE
    // =========================================================================

    fun downloadLog(
        type: DaemonType,
        fragment: Fragment,
        daemonsViewModel: DaemonsViewModel
    ) {
        val ctx = fragment.context ?: return
        val localizedName = type.localizedName(ctx)

        if (LogUploader.isUploadConfigured()) {
            val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_send_log, null)
            dialogView.findViewById<TextView>(R.id.sendLogSubtitle)?.text =
                ctx.getString(R.string.logs_send_subtitle, localizedName)

            val dialog = MaterialAlertDialogBuilder(ctx, R.style.Theme_Overdrive_M3_Dialog)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .create()

            dialogView.findViewById<View>(R.id.optionUpload)?.setOnClickListener {
                dialog.dismiss()
                uploadDaemonLog(type, localizedName, fragment)
            }
            dialogView.findViewById<View>(R.id.optionShare)?.setOnClickListener {
                dialog.dismiss()
                shareDaemonLog(type, fragment, daemonsViewModel)
            }
            dialog.show()
            return
        }
        shareDaemonLog(type, fragment, daemonsViewModel)
    }

    private fun uploadDaemonLog(type: DaemonType, localizedName: String, fragment: Fragment) {
        val ctx = fragment.context ?: return
        val activity = fragment.activity ?: return
        val daemonKey = DaemonAdapter.daemonLogKey(type) ?: run {
            Toast.makeText(ctx, ctx.getString(R.string.toast_log_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        val progressView = LayoutInflater.from(ctx).inflate(R.layout.dialog_log_uploading, null)
        val progressDialog = MaterialAlertDialogBuilder(ctx, R.style.Theme_Overdrive_M3_Dialog)
            .setView(progressView)
            .setCancelable(false)
            .show()

        Thread {
            val req = JSONObject().apply {
                put("command", "UPLOAD_LOG")
                put("daemon", daemonKey)
            }
            val resp = DaemonIpcClient.send(req, 35_000)
            activity.runOnUiThread {
                progressDialog.dismiss()
                if (!fragment.isAdded) return@runOnUiThread
                val ctx2 = fragment.context ?: return@runOnUiThread
                if (resp == null || !resp.optBoolean("success", false)) {
                    val err = resp?.optString("error") ?: ctx2.getString(R.string.errors_network)
                    Toast.makeText(ctx2, ctx2.getString(R.string.toast_log_save_failed, err), Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                val code = resp.optString("code", "")
                showLogUploadedDialog(ctx2, code)
            }
        }.start()
    }

    private fun showLogUploadedDialog(ctx: Context, code: String) {
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_log_uploaded, null)
        view.findViewById<TextView>(R.id.uploadedCode)?.text = code

        fun copyCode() {
            val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clip?.setPrimaryClip(ClipData.newPlainText("log code", code))
            Toast.makeText(ctx, R.string.toast_url_copied_short, Toast.LENGTH_SHORT).show()
        }

        MaterialAlertDialogBuilder(ctx, R.style.Theme_Overdrive_M3_Dialog)
            .setView(view)
            .setPositiveButton(R.string.logs_copy_code) { _, _ -> copyCode() }
            .setNegativeButton(R.string.logs_done, null)
            .show()

        view.findViewById<View>(R.id.codeCard)?.setOnClickListener { copyCode() }
    }

    private fun shareDaemonLog(type: DaemonType, fragment: Fragment, daemonsViewModel: DaemonsViewModel) {
        val ctx = fragment.context ?: return
        val activity = fragment.activity ?: return
        val logPath = DaemonAdapter.getLogFilePath(type) ?: return
        val daemonName = type.displayName.replace(" ", "_").lowercase(Locale.ROOT)
        val localizedName = type.localizedName(ctx)

        val adb = daemonsViewModel.daemonStartupManager?.adbLauncher
        if (adb == null) {
            Toast.makeText(ctx, ctx.getString(R.string.toast_daemon_manager_unavailable), Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(ctx, ctx.getString(R.string.toast_fetching_log, localizedName), Toast.LENGTH_SHORT).show()

        adb.executeShellCommand(
            "wc -l < $logPath 2>/dev/null; echo '---SEPARATOR---'; tail -10000 $logPath 2>/dev/null",
            object : AdbDaemonLauncher.LaunchCallback {
                override fun onLog(message: String) {
                    activity.runOnUiThread {
                        if (!fragment.isAdded) return@runOnUiThread
                        if (message.isBlank()) {
                            Toast.makeText(ctx, ctx.getString(R.string.toast_log_empty_or_missing), Toast.LENGTH_SHORT).show()
                            return@runOnUiThread
                        }

                        try {
                            val parts = message.split("---SEPARATOR---", limit = 2)
                            val totalLines = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
                            val logContent = parts.getOrNull(1)?.trimStart('\n') ?: message

                            if (logContent.isBlank()) {
                                Toast.makeText(ctx, ctx.getString(R.string.toast_log_empty), Toast.LENGTH_SHORT).show()
                                return@runOnUiThread
                            }

                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val fileName = "${daemonName}_${timestamp}.log"
                            val cacheDir = File(ctx.cacheDir, "logs")
                            cacheDir.mkdirs()
                            val logFile = File(cacheDir, fileName)

                            val header = buildString {
                                appendLine(ctx.getString(R.string.log_header_title, localizedName))
                                appendLine(ctx.getString(R.string.log_header_source, logPath))
                                appendLine(ctx.getString(R.string.log_header_exported, Date().toString()))
                                if (totalLines > 10000) {
                                    appendLine(ctx.getString(R.string.log_header_truncated, totalLines))
                                }
                                appendLine("===")
                                appendLine()
                            }
                            logFile.writeText(header + logContent)

                            val uri = FileProvider.getUriForFile(
                                ctx,
                                "${ctx.packageName}.fileprovider",
                                logFile
                            )

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                this.type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, ctx.getString(R.string.log_share_title, localizedName, timestamp))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                clipData = ClipData.newRawUri(null, uri)
                            }
                            fragment.startActivity(Intent.createChooser(shareIntent, ctx.getString(R.string.log_share_chooser, localizedName)))
                        } catch (e: Exception) {
                            Toast.makeText(ctx, ctx.getString(R.string.toast_log_save_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onLaunched() {}

                override fun onError(error: String) {
                    activity.runOnUiThread {
                        if (!fragment.isAdded) return@runOnUiThread
                        Toast.makeText(ctx, ctx.getString(R.string.toast_log_not_found), Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}
