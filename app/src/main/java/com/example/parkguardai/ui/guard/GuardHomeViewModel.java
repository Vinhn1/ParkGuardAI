package com.example.parkguardai.ui.guard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.parkguardai.data.datasource.FirestoreAccessGrantDataSource;
import com.example.parkguardai.data.datasource.FirebaseEmailAuthDataSource;
import com.example.parkguardai.data.model.GuardDraft;
import com.example.parkguardai.data.repository.GuardDraftStore;
import com.example.parkguardai.data.repository.ViolationRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Kiểm quyền bảo vệ từ máy chủ mỗi khi vào khu vực tác nghiệp. */
public final class GuardHomeViewModel extends AndroidViewModel {
    public static final class State {
        public final boolean loading;
        public final boolean authorized;
        public final boolean revoked;
        public final String email;
        public final List<String> scopes;

        State(boolean loading, boolean authorized, boolean revoked,
              String email, List<String> scopes) {
            this.loading = loading;
            this.authorized = authorized;
            this.revoked = revoked;
            this.email = email;
            this.scopes = scopes;
        }
    }

    public static final class RecordsState {
        public final boolean loading;
        public final boolean error;
        public final List<DocumentSnapshot> records;
        public final List<GuardDraft> drafts;
        RecordsState(boolean loading, boolean error, List<DocumentSnapshot> records,
                     List<GuardDraft> drafts) {
            this.loading = loading;
            this.error = error;
            this.records = records;
            this.drafts = drafts;
        }
    }

    private final FirestoreAccessGrantDataSource grants =
            new FirestoreAccessGrantDataSource(FirebaseFirestore.getInstance());
    private final FirebaseEmailAuthDataSource auth =
            new FirebaseEmailAuthDataSource(FirebaseAuth.getInstance());
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final MutableLiveData<RecordsState> records = new MutableLiveData<>();
    private final ViolationRepository violations;
    private final GuardDraftStore drafts;
    private long accessRequestVersion;
    private long recordsRequestVersion;

    public GuardHomeViewModel(@NonNull Application application) {
        super(application);
        violations = new ViolationRepository(application);
        drafts = new GuardDraftStore(application);
    }

    public LiveData<State> getState() { return state; }
    public LiveData<RecordsState> getRecords() { return records; }

    public Task<Void> sendPasswordReset() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            return Tasks.forException(new IllegalStateException("Email unavailable"));
        }
        return auth.sendPasswordResetEmail(user.getEmail());
    }

    public void refresh() {
        long requestVersion = ++accessRequestVersion;
        ++recordsRequestVersion;
        records.setValue(new RecordsState(true, false, null, new ArrayList<>()));
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !user.isEmailVerified()) {
            state.setValue(new State(false, false, true, "", null));
            return;
        }
        String uid = user.getUid();
        String email = user.getEmail() == null ? "" : user.getEmail();
        state.setValue(new State(true, false, false, email, null));
        grants.getSnapshotFromServer(uid).addOnCompleteListener(task -> {
            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
            if (requestVersion != accessRequestVersion
                    || current == null || !uid.equals(current.getUid())) return;
            if (!task.isSuccessful()) {
                state.setValue(new State(false, false, false, email, null));
                records.setValue(new RecordsState(false, true, null, new ArrayList<>()));
                return;
            }
            FirestoreAccessGrantDataSource.Snapshot snapshot = task.getResult();
            boolean authorized = snapshot.access == FirestoreAccessGrantDataSource.Access.GUARD;
            state.setValue(new State(false, authorized, !authorized, email,
                    authorized ? snapshot.scopes : null));
            if (authorized) refreshRecords();
            else records.setValue(new RecordsState(false, true, null, new ArrayList<>()));
        });
    }

    public void refreshRecords() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        long requestVersion = ++recordsRequestVersion;
        List<GuardDraft> local = new ArrayList<>();
        local.addAll(drafts.list(uid));
        records.setValue(new RecordsState(true, false, null, local));
        violations.recentForCurrentGuard().addOnCompleteListener(task -> {
            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
            State access = state.getValue();
            if (requestVersion != recordsRequestVersion || current == null
                    || !uid.equals(current.getUid()) || access == null || !access.authorized) return;
            records.setValue(new RecordsState(false, !task.isSuccessful(),
                    task.isSuccessful() ? task.getResult() : null, local));
        });
    }

    public boolean deleteDraft(GuardDraft draft) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return false;
        String uid = user.getUid();
        try {
            boolean deleted = drafts.delete(draft.id, uid);
            if (deleted) refreshRecords();
            return deleted;
        } catch (IOException failure) { return false; }
    }
}
