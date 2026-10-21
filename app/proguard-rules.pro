# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\ocloc\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep all model and entity classes in com.resort.control for Firebase Firestore deserialization and Room
-keep class com.resort.control.** { *; }
-keepclassmembers class com.resort.control.** {
    <init>();
}
