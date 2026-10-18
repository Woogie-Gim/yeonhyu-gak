import com.gsw.yeonhyugak.buildlogic.JAVA_VERSION
import com.gsw.yeonhyugak.buildlogic.configureStaticAnalysis
import com.gsw.yeonhyugak.buildlogic.lib
import com.gsw.yeonhyugak.buildlogic.libs
import com.gsw.yeonhyugak.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

// 순수 Kotlin 모듈: 안드로이드 의존 없음
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("kotlin-jvm"))
        pluginManager.apply("com.android.lint")
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JAVA_VERSION
            targetCompatibility = JAVA_VERSION
        }
        extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions.jvmTarget.set(JvmTarget.fromTarget(JAVA_VERSION.toString()))
        }
        configureStaticAnalysis()
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
        dependencies {
            add("testImplementation", platform(libs.lib("junit5-bom")))
            add("testImplementation", libs.lib("junit5-jupiter"))
            add("testRuntimeOnly", libs.lib("junit5-platform-launcher"))
            add("testImplementation", libs.lib("kotest-runner-junit5"))
            add("testImplementation", libs.lib("kotest-assertions-core"))
            add("testImplementation", libs.lib("kotest-property"))
        }
    }
}
