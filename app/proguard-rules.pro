# Project-specific R8 rules are intentionally empty.
# Firebase, Room, Compose, and Media3 supply their own consumer rules.

# Hilt generated components and injection metadata.
-keep class dagger.hilt.** { *; }
-keep class com.elwataniatv.app.**_HiltModules { *; }
-keep class com.elwataniatv.app.**_GeneratedInjector { *; }

# Room entities, DAOs, and generated database implementation.
-keep class com.elwataniatv.app.data.local.** { *; }

# Firebase serialization and Media3 playback models.
-keepattributes Signature,*Annotation*
-keep class com.google.firebase.** { *; }
-keep class androidx.media3.** { *; }
