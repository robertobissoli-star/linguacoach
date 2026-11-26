-keepclassmembers class com.linguacoach.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.linguacoach.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.linguacoach.app.data.**$$serializer { *; }
