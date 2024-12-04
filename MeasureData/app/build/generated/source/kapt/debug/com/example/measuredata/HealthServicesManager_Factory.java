package com.example.measuredata;

import android.content.Context;
import androidx.health.services.client.HealthServicesClient;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class HealthServicesManager_Factory implements Factory<HealthServicesManager> {
  private final Provider<Context> contextProvider;

  private final Provider<HealthServicesClient> healthServicesClientProvider;

  public HealthServicesManager_Factory(Provider<Context> contextProvider,
      Provider<HealthServicesClient> healthServicesClientProvider) {
    this.contextProvider = contextProvider;
    this.healthServicesClientProvider = healthServicesClientProvider;
  }

  @Override
  public HealthServicesManager get() {
    return newInstance(contextProvider.get(), healthServicesClientProvider.get());
  }

  public static HealthServicesManager_Factory create(Provider<Context> contextProvider,
      Provider<HealthServicesClient> healthServicesClientProvider) {
    return new HealthServicesManager_Factory(contextProvider, healthServicesClientProvider);
  }

  public static HealthServicesManager newInstance(Context context,
      HealthServicesClient healthServicesClient) {
    return new HealthServicesManager(context, healthServicesClient);
  }
}
