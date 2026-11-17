package app.ash.launcher.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import app.ash.launcher.MainViewModel
import app.ash.launcher.R
import app.ash.launcher.data.Prefs
import app.ash.launcher.databinding.FragmentHomeBinding
import app.ash.launcher.helper.expandNotificationDrawer

class HomeFragment : BaseFragment(), View.OnClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        initPager()
        initObservers()
        binding.setDefaultLauncher.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        binding.homePager.setCurrentItem(HOME_PAGE, false)
        viewModel.isOlauncherDefault()
        if (prefs.showStatusBar) showStatusBar()
        else hideStatusBar()
    }

    override fun onClick(view: View) {
        if (view.id == R.id.setDefaultLauncher)
            viewModel.resetLauncherLiveData.call()
    }

    private fun initPager() {
        binding.homePager.adapter = HomePagerAdapter(
            requireContext(),
            object : HomePagerAdapter.GestureCallbacks {
                override fun onSwipeUp() {
                    openAppDrawer()
                }

                override fun onSwipeDown() {
                    expandNotificationDrawer(requireContext())
                }

                override fun onLongPress() {
                    openSettings()
                }
            }
        )
        binding.homePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicator(position)
            }
        })
        updateIndicator(HOME_PAGE)
    }

    private fun initObservers() {
        viewModel.isOlauncherDefault.observe(viewLifecycleOwner) { isDefault ->
            binding.setDefaultLauncher.isVisible = isDefault.not() && prefs.hideSetDefaultLauncher.not()
        }
    }

    private fun updateIndicator(page: Int) {
        val marks = (0 until HomePagerAdapter.PAGE_COUNT).joinToString("") {
            if (it == page) "●" else "○"
        }
        binding.pageIndicator.text = "[$marks]"
    }

    private fun openAppDrawer() {
        findNavController().navigate(R.id.action_mainFragment_to_appListFragment)
    }

    private fun openSettings() {
        findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
    }

    private fun showStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars())
        else
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
        else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val HOME_PAGE = 0
    }
}
