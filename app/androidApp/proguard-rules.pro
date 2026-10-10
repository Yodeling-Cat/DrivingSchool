# Rules for the minified release build.
#
# R8 is on for release (see build.gradle.kts), so anything the app reaches without a static
# reference has to be kept here by hand. Everything else — Compose, Koin, kotlinx.serialization,
# Coil, AndroidX — ships its own consumer rules and needs nothing from this file.

# The shared module reads the application's build fields by name, because it is a library and
# generates no BuildConfig of its own:
#
#   app/shared/src/androidMain/.../AppVersion.android.kt    Class.forName(...).getField("VERSION_NAME")
#   app/shared/src/androidMain/.../DebugBuild.android.kt    Class.forName(...).getField("DEBUG")
#
# Those are the app's only reflective lookups. Without this the fields are shrunk away and both
# callers quietly answer null and false — the About screen loses its version line, and the debug
# build stops being recognized as one.
-keep class com.spiraclestudios.autoskola.BuildConfig { *; }

# The ads SDK pulls in WorkManager 2.7 and with it Room 2.2.5, whose consumer rule keeps every
# RoomDatabase subclass but not its constructor. Room creates the generated WorkDatabase_Impl by
# reflection, and R8's full mode drops a constructor that nothing calls directly, so the released
# app crashed on launch with "Failed to create an instance of androidx.work.impl.WorkDatabase"
# while debug builds, which are not minified, ran fine. Newer Room ships this rule itself.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# Keep the source file and line number of every frame, so a crash report from a release build
# points at a line rather than at nothing. The mapping file PostHog reads needs this to be useful.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
