import com.android.build.api.dsl.ApplicationExtension
import com.gsw.yeonhyugak.buildlogic.TARGET_SDK
import com.gsw.yeonhyugak.buildlogic.configureAndroidCommon
import com.gsw.yeonhyugak.buildlogic.configureStaticAnalysis
import com.gsw.yeonhyugak.buildlogic.libs
import com.gsw.yeonhyugak.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("android-application"))
        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(this)
            defaultConfig.targetSdk = TARGET_SDK
        }
        configureStaticAnalysis()
    }
}
