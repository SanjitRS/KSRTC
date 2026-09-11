# Add project specific ProGuard rules here.
-keep class net.kibotu.geofencerelay.model.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-dontwarn org.eclipse.paho.client.mqttv3.**
