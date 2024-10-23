package com.example.measuredata;

import android.content.SharedPreferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
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
public final class MainViewModel_Factory implements Factory<MainViewModel> {
  private final Provider<HealthServicesManager> healthServicesManagerProvider;

  private final Provider<SharedPreferences> sharedPreferencesProvider;

  public MainViewModel_Factory(Provider<HealthServicesManager> healthServicesManagerProvider,
      Provider<SharedPreferences> sharedPreferencesProvider) {
    this.healthServicesManagerProvider = healthServicesManagerProvider;
    this.sharedPreferencesProvider = sharedPreferencesProvider;
  }

  @Override
  public MainViewModel get() {
    return newInstance(healthServicesManagerProvider.get(), sharedPreferencesProvider.get());
  }

  public static MainViewModel_Factory create(
      Provider<HealthServicesManager> healthServicesManagerProvider,
      Provider<SharedPreferences> sharedPreferencesProvider) {
    return new MainViewModel_Factory(healthServicesManagerProvider, sharedPreferencesProvider);
  }

  public static MainViewModel newInstance(HealthServicesManager healthServicesManager,
      SharedPreferences sharedPreferences) {
    return new MainViewModel(healthServicesManager, sharedPreferences);
  }
}
