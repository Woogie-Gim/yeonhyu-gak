import com.android.build.api.dsl.LibraryExtension
import com.gsw.yeonhyugak.buildlogic.TARGET_SDK
import com.gsw.yeonhyugak.buildlogic.configureAndroidCommon
import com.gsw.yeonhyugak.buildlogic.configureStaticAnalysis
import com.gsw.yeonhyugak.buildlogic.lib
import com.gsw.yeonhyugak.buildlogic.libs
import com.gsw.yeonhyugak.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("android-library"))
        extensions.configure<LibraryExtension> {
            configureAndroidCommon(this)
            lint.targetSdk = TARGET_SDK
            testOptions.targetSdk = TARGET_SDK
        }
        configureStaticAnalysis()
        dependencies {
            add("testImplementation", libs.lib("junit"))
        }
    }
}
