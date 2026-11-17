package app.ash.launcher.ui

import android.os.Bundle
import android.os.Process
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import app.ash.launcher.BuildConfig
import app.ash.launcher.MainViewModel
import app.ash.launcher.R
import app.ash.launcher.data.Prefs
import app.ash.launcher.databinding.DialogTextSizeBinding
import app.ash.launcher.databinding.FragmentSettingsBinding
import app.ash.launcher.data.Constants
import app.ash.launcher.helper.OlDialog
import app.ash.launcher.helper.createDialog
import app.ash.launcher.helper.hideStatusBar
import app.ash.launcher.helper.isTablet
import app.ash.launcher.helper.openAppInfo
import app.ash.launcher.helper.openUrl
import app.ash.launcher.helper.showPopupMenu
import app.ash.launcher.helper.showStatusBar

class SettingsFragment : BaseFragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var dialog: OlDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
        viewModel.isOlauncherDefault()

        populateSetDefault()
        populatePageCount()
        populateStartPage()
        populateSplash()
        populateStatusBar()
        populateAppThemeText()
        populateTextSize()
        binding.footer.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)

        initClickListeners()
        initObservers()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.setLauncher -> viewModel.resetLauncherLiveData.call()
            R.id.github -> requireContext().openUrl(Constants.URL_OLAUNCHER_GITHUB)
            R.id.appInfo -> openAppInfo(requireContext(), Process.myUserHandle(), BuildConfig.APPLICATION_ID)
            R.id.pageCount -> showPageCountMenu(view)
            R.id.startPage -> showStartPageMenu(view)
            R.id.asciiSplash -> toggleSplash()
            R.id.statusBar -> toggleStatusBar()
            R.id.appThemeText -> showAppThemeMenu(view, showSystem = false)
            R.id.textSizeValue -> showTextSizeDialog()
        }
    }

    override fun onLongClick(view: View): Boolean {
        if (view.id == R.id.appThemeText) showAppThemeMenu(view, showSystem = true)
        return true
    }

    private fun initClickListeners() {
        binding.setLauncher.setOnClickListener(this)
        binding.github.setOnClickListener(this)
        binding.appInfo.setOnClickListener(this)
        binding.pageCount.setOnClickListener(this)
        binding.startPage.setOnClickListener(this)
        binding.asciiSplash.setOnClickListener(this)
        binding.statusBar.setOnClickListener(this)
        binding.appThemeText.setOnClickListener(this)
        binding.appThemeText.setOnLongClickListener(this)
        binding.textSizeValue.setOnClickListener(this)
    }

    private fun initObservers() {
        viewModel.isOlauncherDefault.observe(viewLifecycleOwner) {
            populateSetDefault()
        }
    }

    private fun populateSetDefault() {
        binding.setLauncher.text = getString(
            if (viewModel.isOlauncherDefault.value == true) R.string.change_default_launcher
            else R.string.settings_prompt_set_default
        )
    }

    private fun showPageCountMenu(anchor: View) {
        anchor.showPopupMenu(
            configure = { menu ->
                for (num in Prefs.MIN_PAGE_COUNT..Prefs.MAX_PAGE_COUNT)
                    menu.add(Menu.NONE, num, num, num.toString())
            }
        ) { item ->
            prefs.pageCount = item.itemId
            if (prefs.startPage > prefs.pageCount) prefs.startPage = prefs.pageCount
            populatePageCount()
            populateStartPage()
        }
    }

    private fun showStartPageMenu(anchor: View) {
        anchor.showPopupMenu(
            configure = { menu ->
                for (num in 1..prefs.pageCount) menu.add(Menu.NONE, num, num, num.toString())
            }
        ) { item ->
            prefs.startPage = item.itemId
            populateStartPage()
        }
    }

    private fun populatePageCount() {
        binding.pageCount.text = prefs.pageCount.toString()
    }

    private fun populateStartPage() {
        binding.startPage.text = prefs.startPage.toString()
    }

    private fun toggleSplash() {
        prefs.asciiSplash = !prefs.asciiSplash
        populateSplash()
    }

    private fun populateSplash() {
        binding.asciiSplash.text = getString(
            if (prefs.asciiSplash) R.string.settings_on else R.string.settings_off
        )
    }

    private fun toggleStatusBar() {
        prefs.showStatusBar = !prefs.showStatusBar
        populateStatusBar()
    }

    private fun populateStatusBar() {
        if (prefs.showStatusBar) {
            requireActivity().window.showStatusBar()
            binding.statusBar.text = getString(R.string.settings_on)
        } else {
            requireActivity().window.hideStatusBar()
            binding.statusBar.text = getString(R.string.settings_off)
        }
    }

    private fun showAppThemeMenu(anchor: View, showSystem: Boolean) {
        anchor.showPopupMenu(
            R.menu.app_theme,
            configure = { menu -> menu.findItem(R.id.themeSystem).isVisible = showSystem }
        ) { item ->
            when (item.itemId) {
                R.id.themeLight -> updateTheme(AppCompatDelegate.MODE_NIGHT_NO)
                R.id.themeDark -> updateTheme(AppCompatDelegate.MODE_NIGHT_YES)
                R.id.themeSystem -> updateTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    private fun updateTheme(appTheme: Int) {
        populateAppThemeText(appTheme)
        if (prefs.appTheme == appTheme) return
        prefs.appTheme = appTheme
        AppCompatDelegate.setDefaultNightMode(appTheme)
        requireActivity().recreate()
    }

    private fun populateAppThemeText(appTheme: Int = prefs.appTheme) {
        binding.appThemeText.text = getString(
            when (appTheme) {
                AppCompatDelegate.MODE_NIGHT_YES -> R.string.settings_dark
                AppCompatDelegate.MODE_NIGHT_NO -> R.string.settings_light
                else -> R.string.settings_system
            }
        )
    }

    private fun showTextSizeDialog() {
        var stepper: DialogTextSizeBinding? = null
        val dialog = requireContext().createDialog(R.string.text_size, R.string.okay) { container ->
            DialogTextSizeBinding.inflate(layoutInflater, container, false).also { stepper = it }.root
        }
        stepper?.apply {
            textSizeCurrent.text = formatScale(pendingOrCurrentTextSizeScale())
            textSizeMinus.setOnClickListener { adjustTextSizePreview(-0.1f, this) }
            textSizePlus.setOnClickListener { adjustTextSizePreview(0.1f, this) }
        }
        dialog.setOnDismissListener { applyTextSizeScale() }
        showDialog(dialog)
    }

    private fun showDialog(newDialog: OlDialog) {
        dialog?.dismiss()
        dialog = newDialog
        newDialog.showRespectingStatusBar()
    }

    private var pendingTextSizeScale: Float = -1f

    private fun pendingOrCurrentTextSizeScale(): Float =
        if (pendingTextSizeScale > 0) pendingTextSizeScale else prefs.textSizeScale

    private fun formatScale(scale: Float): String = String.format("%.1f", scale)

    private fun adjustTextSizePreview(delta: Float, dialogBinding: DialogTextSizeBinding) {
        val maxScale = if (isTablet(requireContext())) 2.0f else 1.5f
        val current = pendingOrCurrentTextSizeScale()
        val newScale = Math.round((current + delta) * 10f) / 10f
        val clamped = newScale.coerceIn(0.5f, maxScale)
        if (clamped == current) return
        pendingTextSizeScale = clamped
        val formatted = formatScale(clamped)
        binding.textSizeValue.text = formatted
        dialogBinding.textSizeCurrent.text = formatted
    }

    private fun applyTextSizeScale() {
        if (pendingTextSizeScale < 0 || prefs.textSizeScale == pendingTextSizeScale) {
            pendingTextSizeScale = -1f
            return
        }
        prefs.textSizeScale = pendingTextSizeScale
        pendingTextSizeScale = -1f
        val activity = activity ?: return
        if (activity.isChangingConfigurations.not())
            activity.recreate()
    }

    private fun populateTextSize() {
        binding.textSizeValue.text = formatScale(prefs.textSizeScale)
    }

    override fun onDestroyView() {
        dialog?.dismiss()
        dialog = null
        applyTextSizeScale()
        super.onDestroyView()
        _binding = null
    }
}
