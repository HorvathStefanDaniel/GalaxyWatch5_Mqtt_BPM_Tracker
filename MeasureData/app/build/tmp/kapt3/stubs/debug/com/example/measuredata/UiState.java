package com.example.measuredata;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0006\u0003\u0004\u0005\u0006\u0007\bB\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0006\t\n\u000b\f\r\u000e\u00a8\u0006\u000f"}, d2 = {"Lcom/example/measuredata/UiState;", "", "()V", "ConnectedToMQTT", "ConnectingToMQTT", "FailedToConnectMQTT", "HeartRateAvailable", "HeartRateNotAvailable", "Startup", "Lcom/example/measuredata/UiState$ConnectedToMQTT;", "Lcom/example/measuredata/UiState$ConnectingToMQTT;", "Lcom/example/measuredata/UiState$FailedToConnectMQTT;", "Lcom/example/measuredata/UiState$HeartRateAvailable;", "Lcom/example/measuredata/UiState$HeartRateNotAvailable;", "Lcom/example/measuredata/UiState$Startup;", "app_debug"})
public abstract class UiState {
    
    private UiState() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/measuredata/UiState$ConnectedToMQTT;", "Lcom/example/measuredata/UiState;", "()V", "app_debug"})
    public static final class ConnectedToMQTT extends com.example.measuredata.UiState {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.measuredata.UiState.ConnectedToMQTT INSTANCE = null;
        
        private ConnectedToMQTT() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/measuredata/UiState$ConnectingToMQTT;", "Lcom/example/measuredata/UiState;", "()V", "app_debug"})
    public static final class ConnectingToMQTT extends com.example.measuredata.UiState {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.measuredata.UiState.ConnectingToMQTT INSTANCE = null;
        
        private ConnectingToMQTT() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/measuredata/UiState$FailedToConnectMQTT;", "Lcom/example/measuredata/UiState;", "()V", "app_debug"})
    public static final class FailedToConnectMQTT extends com.example.measuredata.UiState {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.measuredata.UiState.FailedToConnectMQTT INSTANCE = null;
        
        private FailedToConnectMQTT() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/measuredata/UiState$HeartRateAvailable;", "Lcom/example/measuredata/UiState;", "()V", "app_debug"})
    public static final class HeartRateAvailable extends com.example.measuredata.UiState {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.measuredata.UiState.HeartRateAvailable INSTANCE = null;
        
        private HeartRateAvailable() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/measuredata/UiState$HeartRateNotAvailable;", "Lcom/example/measuredata/UiState;", "()V", "app_debug"})
    public static final class HeartRateNotAvailable extends com.example.measuredata.UiState {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.measuredata.UiState.HeartRateNotAvailable INSTANCE = null;
        
        private HeartRateNotAvailable() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/example/measuredata/UiState$Startup;", "Lcom/example/measuredata/UiState;", "()V", "app_debug"})
    public static final class Startup extends com.example.measuredata.UiState {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.measuredata.UiState.Startup INSTANCE = null;
        
        private Startup() {
        }
    }
}