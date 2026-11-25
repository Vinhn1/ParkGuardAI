package com.example.parkguardai.ui.guard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.parkguardai.data.repository.ViolationRepository;
import com.google.firebase.firestore.DocumentSnapshot;

/** Tải hồ sơ và metadata ảnh trực tiếp từ máy chủ theo Rules. */
public final class GuardDetailViewModel extends AndroidViewModel {
    public static final class State {
        public final boolean loading;
        public final boolean error;
        public final DocumentSnapshot record;
        public final DocumentSnapshot primary;
        public final DocumentSnapshot crop;
        State(boolean loading, boolean error, DocumentSnapshot record,
              DocumentSnapshot primary, DocumentSnapshot crop) {
            this.loading = loading;
            this.error = error;
            this.record = record;
            this.primary = primary;
            this.crop = crop;
        }
    }

    private final ViolationRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>();

    public GuardDetailViewModel(@NonNull Application application) {
        super(application);
        repository = new ViolationRepository(application);
    }

    public LiveData<State> getState() { return state; }

    public void load(String id) {
        state.setValue(new State(true, false, null, null, null));
        repository.getRecord(id).addOnCompleteListener(recordTask -> {
            if (!recordTask.isSuccessful() || !recordTask.getResult().exists()) {
                state.setValue(new State(false, true, null, null, null));
                return;
            }
            DocumentSnapshot record = recordTask.getResult();
            repository.getPrimaryEvidence(id).addOnCompleteListener(primaryTask -> {
                if (!primaryTask.isSuccessful() || !primaryTask.getResult().exists()) {
                    state.setValue(new State(false, true, null, null, null));
                    return;
                }
                DocumentSnapshot primary = primaryTask.getResult();
                repository.getCropEvidence(id).addOnCompleteListener(cropTask -> {
                    state.setValue(new State(false, false, record, primary,
                            cropTask.isSuccessful() && cropTask.getResult().exists()
                                    ? cropTask.getResult() : null));
                });
            });
        });
    }
}
