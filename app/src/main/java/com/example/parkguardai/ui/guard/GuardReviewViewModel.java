package com.example.parkguardai.ui.guard;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.parkguardai.data.datasource.FirestoreAccessGrantDataSource;
import com.example.parkguardai.data.model.GuardDraft;
import com.example.parkguardai.data.repository.ViolationRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/** Điều phối quyền, upload ảnh Cloudinary và lưu hồ sơ Firestore thật cho màn rà soát. */
public final class GuardReviewViewModel extends AndroidViewModel {
    public static final class SubmitState {
        public final boolean loading;
        public final boolean success;
        public final boolean error;
        public final ViolationRepository.Stage stage;
        public final String recordId;
        SubmitState(boolean loading, boolean success, boolean error,
                    ViolationRepository.Stage stage, String recordId) {
            this.loading = loading;
            this.success = success;
            this.error = error;
            this.stage = stage;
            this.recordId = recordId;
        }
    }

    private final FirestoreAccessGrantDataSource grants =
            new FirestoreAccessGrantDataSource(FirebaseFirestore.getInstance());
    private final ViolationRepository repository;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<List<String>> scopes = new MutableLiveData<>();
    private final MutableLiveData<SubmitState> submit = new MutableLiveData<>(
            new SubmitState(false, false, false, null, null));

    public GuardReviewViewModel(@NonNull Application application) {
        super(application);
        repository = new ViolationRepository(application);
    }

    public LiveData<List<String>> getScopes() { return scopes; }
    public LiveData<SubmitState> getSubmit() { return submit; }

    public void consumeSuccess() {
        submit.setValue(new SubmitState(false, false, false, null, null));
    }

    public void loadScopes() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            scopes.setValue(null);
            return;
        }
        grants.getSnapshotFromServer(user.getUid())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().access
                            == FirestoreAccessGrantDataSource.Access.GUARD) {
                        scopes.setValue(task.getResult().scopes);
                    } else {
                        scopes.setValue(null);
                    }
                });
    }

    public void submit(GuardDraft draft) {
        SubmitState current = submit.getValue();
        if (current != null && current.loading) return;

        submit.setValue(new SubmitState(true, false, false,
                ViolationRepository.Stage.CHECKING_ACCESS, null));
        repository.submit(draft, stage -> main.post(() -> {
                    SubmitState active = submit.getValue();
                    if (active != null && active.loading
                            && stage != ViolationRepository.Stage.COMPLETE) {
                        submit.setValue(new SubmitState(true, false, false, stage, null));
                    }
                }))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        submit.setValue(new SubmitState(false, true, false,
                                ViolationRepository.Stage.COMPLETE, task.getResult()));
                    } else {
                        submit.setValue(new SubmitState(false, false, true, null, null));
                    }
                });
    }
}
