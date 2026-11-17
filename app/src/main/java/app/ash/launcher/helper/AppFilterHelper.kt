package app.ash.launcher.helper

import app.ash.launcher.data.AppModel

interface AppFilterHelper {
    fun onAppFiltered(items:List<AppModel>)
}