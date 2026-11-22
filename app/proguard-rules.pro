# Keep Firestore @Keep models and Room entities intact under R8 minification
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class com.example.data.remote.** {
    *;
}
-keepclassmembers class com.example.data.local.** {
    *;
}

# Preserve Firebase Firestore annotations and serialization metadata
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
}

# Preserve Google Credential Manager & Play Services classes
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
