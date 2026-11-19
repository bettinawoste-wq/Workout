package com.wob.workout.ui.settings;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.wob.workout.AppContainer;
import com.wob.workout.backup.Backup;
import com.wob.workout.backup.BackupPreview;
import com.wob.workout.backup.BackupRefused;
import com.wob.workout.data.Event;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;

/**
 * The settings screen's Data section: export to a file and read one back, both on the io
 * executor. Between the preview and the user's choice it keeps the chosen file.
 */
public class BackupViewModel extends ViewModel {

    private static final String TAG = "BackupViewModel";

    /** How an export or import ended, shown once. */
    public enum Outcome {
        EXPORTED, EXPORT_FAILED, IMPORTED, IMPORT_FAILED
    }

    private final Backup backup;
    private final ContentResolver resolver;
    private final ExecutorService ioExecutor;

    private final MutableLiveData<Event<BackupPreview>> preview = new MutableLiveData<>();
    private final MutableLiveData<Event<Outcome>> outcome = new MutableLiveData<>();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);

    /** The file the shown preview is about; null when none is waiting for a choice. */
    private Uri chosenFile;

    public BackupViewModel(AppContainer container) {
        backup = container.backup;
        resolver = container.appContext.getContentResolver();
        ioExecutor = container.ioExecutor;
    }

    /** What a chosen file holds, or why it is refused; once per file. */
    public LiveData<Event<BackupPreview>> getPreview() {
        return preview;
    }

    public LiveData<Event<Outcome>> getOutcome() {
        return outcome;
    }

    /** True while an export or import runs. */
    public LiveData<Boolean> isBusy() {
        return busy;
    }

    /** Writes the backup into the file chosen in the system's file dialog. */
    public void exportTo(Uri file) {
        busy.setValue(true);
        ioExecutor.execute(() -> {
            Outcome result = Outcome.EXPORTED;
            try (OutputStream out = resolver.openOutputStream(file, "wt")) {
                if (out == null) throw new FileNotFoundException(file.toString());
                backup.export(out);
            } catch (IOException | RuntimeException e) {
                Log.e(TAG, "Export failed", e);
                result = Outcome.EXPORT_FAILED;
            }
            finish(result);
        });
    }

    /** Reads the file chosen in the system's file dialog for its preview; changes nothing. */
    public void open(Uri file) {
        chosenFile = null;
        busy.setValue(true);
        ioExecutor.execute(() -> {
            BackupPreview result;
            try (InputStream in = resolver.openInputStream(file)) {
                if (in == null) throw new FileNotFoundException(file.toString());
                result = backup.preview(in);
            } catch (IOException | RuntimeException e) {
                Log.e(TAG, "Reading the backup failed", e);
                result = BackupPreview.refused(BackupRefused.Reason.UNREADABLE);
            }
            if (result.refusal == null) chosenFile = file;
            busy.postValue(false);
            preview.postValue(new Event<>(result));
        });
    }

    /** Ersetzen: everything on the phone is replaced by the previewed file. */
    public void replace() {
        importChosen(true);
    }

    /** Zusammenführen: the previewed file's contents are added alongside, as copies. */
    public void addAlongside() {
        importChosen(false);
    }

    /** Abbrechen: nothing happens. */
    public void cancel() {
        chosenFile = null;
    }

    private void importChosen(boolean replace) {
        Uri file = chosenFile;
        chosenFile = null;
        if (file == null) return;
        busy.setValue(true);
        ioExecutor.execute(() -> {
            Outcome result = Outcome.IMPORTED;
            try (InputStream in = resolver.openInputStream(file)) {
                if (in == null) throw new FileNotFoundException(file.toString());
                if (replace) {
                    backup.replaceWith(in);
                } else {
                    backup.addAlongside(in);
                }
            } catch (BackupRefused | IOException | RuntimeException e) {
                Log.e(TAG, "Import failed", e);
                result = Outcome.IMPORT_FAILED;
            }
            finish(result);
        });
    }

    private void finish(Outcome result) {
        busy.postValue(false);
        outcome.postValue(new Event<>(result));
    }
}
