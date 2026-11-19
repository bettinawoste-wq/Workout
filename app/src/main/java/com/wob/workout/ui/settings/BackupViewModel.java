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
import com.wob.workout.backup.ImportMode;
import com.wob.workout.data.Event;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
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
            Boolean done = useFile(file, () -> resolver.openOutputStream(file, "wt"), out -> {
                backup.export(out);
                return true;
            }, "Export failed");
            finish(done != null ? Outcome.EXPORTED : Outcome.EXPORT_FAILED);
        });
    }

    /** Reads the file chosen in the system's file dialog for its preview; changes nothing. */
    public void open(Uri file) {
        chosenFile = null;
        busy.setValue(true);
        ioExecutor.execute(() -> {
            BackupPreview result = useFile(file, () -> resolver.openInputStream(file),
                    backup::preview, "Reading the backup failed");
            if (result == null) result = BackupPreview.refused(BackupRefused.Reason.UNREADABLE);
            if (result.refusal == null) chosenFile = file;
            busy.postValue(false);
            preview.postValue(new Event<>(result));
        });
    }

    /** Ersetzen: everything on the phone is replaced by the previewed file. */
    public void replace() {
        importChosen(ImportMode.REPLACE);
    }

    /** Zusammenführen: the previewed file's contents are added alongside, as copies. */
    public void addAlongside() {
        importChosen(ImportMode.ADD_ALONGSIDE);
    }

    /** Abbrechen: nothing happens. */
    public void cancel() {
        chosenFile = null;
    }

    private void importChosen(ImportMode mode) {
        Uri file = chosenFile;
        chosenFile = null;
        if (file == null) return;
        busy.setValue(true);
        ioExecutor.execute(() -> {
            Boolean done = useFile(file, () -> resolver.openInputStream(file), in -> {
                backup.importFrom(in, mode);
                return true;
            }, "Import failed");
            finish(done != null ? Outcome.IMPORTED : Outcome.IMPORT_FAILED);
        });
    }

    private void finish(Outcome result) {
        busy.postValue(false);
        outcome.postValue(new Event<>(result));
    }

    /** Opens a stream on a chosen file. */
    private interface Opening<S extends Closeable> {
        /** @return the stream, or null if the provider has none */
        S open() throws IOException;
    }

    /** What is done with an opened file's stream. */
    private interface Use<S, R> {
        R apply(S stream) throws BackupRefused, IOException;
    }

    /** Opens the file, uses its stream and closes it again.
     *  @return what {@code use} gave, or null if opening or using the file failed; the
     *          failure is logged with {@code failure} */
    private static <S extends Closeable, R> R useFile(Uri file, Opening<S> opening,
            Use<S, R> use, String failure) {
        try (S stream = opening.open()) {
            if (stream == null) throw new FileNotFoundException(file.toString());
            return use.apply(stream);
        } catch (BackupRefused | IOException | RuntimeException e) {
            Log.e(TAG, failure, e);
            return null;
        }
    }
}
