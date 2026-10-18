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
        }
    }
}
