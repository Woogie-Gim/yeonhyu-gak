import com.gsw.yeonhyugak.buildlogic.lib
import com.gsw.yeonhyugak.buildlogic.libs
import com.gsw.yeonhyugak.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("yh-android-library"))
        pluginManager.apply(libs.pluginId("yh-android-compose"))
        pluginManager.apply(libs.pluginId("yh-android-hilt"))
        dependencies {
            add("implementation", project(":core:data"))
            add("implementation", project(":core:ui"))
            add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("hilt-lifecycle-viewmodel-compose"))
            // Compose UI 테스트는 Robolectric에서 실행
            add("testImplementation", libs.lib("androidx-junit"))
            add("testImplementation", libs.lib("robolectric"))
            add("testImplementation", libs.lib("androidx-compose-ui-test-junit4"))
            add("testImplementation", libs.lib("kotlinx-coroutines-test"))
            add("debugImplementation", libs.lib("androidx-compose-ui-test-manifest"))
        }
    }
}
