package com.example.measuredata;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u0000 \r2\u00020\u0001:\u0001\rB\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0007\u001a\u00020\bH\u0086@\u00a2\u0006\u0002\u0010\tJ\u000e\u0010\n\u001a\b\u0012\u0004\u0012\u00020\f0\u000bH\u0007R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000e"}, d2 = {"Lcom/example/measuredata/HealthServicesManager;", "", "healthServicesClient", "Landroidx/health/services/client/HealthServicesClient;", "(Landroidx/health/services/client/HealthServicesClient;)V", "measureClient", "Landroidx/health/services/client/MeasureClient;", "hasHeartRateCapability", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "heartRateMeasureFlow", "Lkotlinx/coroutines/flow/Flow;", "Lcom/example/measuredata/MeasureMessage;", "Companion", "app_debug"})
public final class HealthServicesManager {
    @org.jetbrains.annotations.NotNull()
    private final androidx.health.services.client.MeasureClient measureClient = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "HealthServicesManager";
    @org.jetbrains.annotations.NotNull()
    public static final com.example.measuredata.HealthServicesManager.Companion Companion = null;
    
    @javax.inject.Inject()
    public HealthServicesManager(@org.jetbrains.annotations.NotNull()
    androidx.health.services.client.HealthServicesClient healthServicesClient) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object hasHeartRateCapability(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @kotlinx.coroutines.ExperimentalCoroutinesApi()
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<com.example.measuredata.MeasureMessage> heartRateMeasureFlow() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/example/measuredata/HealthServicesManager$Companion;", "", "()V", "TAG", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}