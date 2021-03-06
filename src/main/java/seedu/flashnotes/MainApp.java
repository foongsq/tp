package seedu.flashnotes;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.stage.Stage;
import seedu.flashnotes.commons.core.Config;
import seedu.flashnotes.commons.core.LogsCenter;
import seedu.flashnotes.commons.core.Version;
import seedu.flashnotes.commons.exceptions.DataConversionException;
import seedu.flashnotes.commons.util.ConfigUtil;
import seedu.flashnotes.commons.util.StringUtil;
import seedu.flashnotes.logic.Logic;
import seedu.flashnotes.logic.LogicManager;
import seedu.flashnotes.model.FlashNotes;
import seedu.flashnotes.model.Model;
import seedu.flashnotes.model.ModelManager;
import seedu.flashnotes.model.ReadOnlyFlashNotes;
import seedu.flashnotes.model.ReadOnlyUserPrefs;
import seedu.flashnotes.model.UserPrefs;
import seedu.flashnotes.model.util.SampleDataUtil;
import seedu.flashnotes.storage.FlashNotesStorage;
import seedu.flashnotes.storage.JsonFlashNotesStorage;
import seedu.flashnotes.storage.JsonUserPrefsStorage;
import seedu.flashnotes.storage.Storage;
import seedu.flashnotes.storage.StorageManager;
import seedu.flashnotes.storage.UserPrefsStorage;
import seedu.flashnotes.ui.Ui;
import seedu.flashnotes.ui.UiManager;

/**
 * Runs the application.
 */
public class MainApp extends Application {

    public static final Version VERSION = new Version(0, 6, 0, true);

    private static final Logger logger = LogsCenter.getLogger(MainApp.class);

    protected Ui ui;
    protected Logic logic;
    protected Storage storage;
    protected Model model;
    protected Config config;

    @Override
    public void init() throws Exception {
        logger.info("=============================[ Initializing FlashNotes ]===========================");
        super.init();

        AppParameters appParameters = AppParameters.parse(getParameters());
        config = initConfig(appParameters.getConfigPath());

        UserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(config.getUserPrefsFilePath());
        UserPrefs userPrefs = initPrefs(userPrefsStorage);
        FlashNotesStorage flashNotesStorage = new JsonFlashNotesStorage(userPrefs.getFlashNotesFilePath());
        storage = new StorageManager(flashNotesStorage, userPrefsStorage);

        initLogging(config);

        model = initModelManager(storage, userPrefs);

        logic = new LogicManager(model, storage);

        ui = new UiManager(logic);
    }

    /**
     * Returns a {@code ModelManager} with the data from {@code storage}'s flashnotes book and {@code userPrefs}. <br>
     * The data from the sample flashnotes book will be used instead if {@code storage}'s flashnotes book is not found,
     * or an empty flashnotes book will be used instead if errors occur when reading {@code storage}'s flashnotes book.
     */
    private Model initModelManager(Storage storage, ReadOnlyUserPrefs userPrefs) {
        Optional<ReadOnlyFlashNotes> flashNotesOptional;
        ReadOnlyFlashNotes initialData;
        try {
            flashNotesOptional = storage.readFlashNotes();
            if (!flashNotesOptional.isPresent()) {
                logger.info("Data file not found. Will be starting with a sample FlashNotes");
            }
            initialData = flashNotesOptional.orElseGet(SampleDataUtil::getSampleFlashNotes);
        } catch (DataConversionException e) {
            logger.warning("Data file not in the correct format. Will be starting with an empty FlashNotes");
            initialData = new FlashNotes();
        } catch (IOException e) {
            logger.warning("Problem while reading from the file. Will be starting with an empty FlashNotes");
            initialData = new FlashNotes();
        }

        return new ModelManager(initialData, userPrefs);
    }

    private void initLogging(Config config) {
        LogsCenter.init(config);
    }

    /**
     * Returns a {@code Config} using the file at {@code configFilePath}. <br>
     * The default file path {@code Config#DEFAULT_CONFIG_FILE} will be used instead
     * if {@code configFilePath} is null.
     */
    protected Config initConfig(Path configFilePath) {
        Config initializedConfig;
        Path configFilePathUsed;

        configFilePathUsed = Config.DEFAULT_CONFIG_FILE;

        if (configFilePath != null) {
            logger.info("Custom Config file specified " + configFilePath);
            configFilePathUsed = configFilePath;
        }

        logger.info("Using config file : " + configFilePathUsed);

        try {
            Optional<Config> configOptional = ConfigUtil.readConfig(configFilePathUsed);
            initializedConfig = configOptional.orElse(new Config());
        } catch (DataConversionException e) {
            logger.warning("Config file at " + configFilePathUsed + " is not in the correct format. "
                    + "Using default config properties");
            initializedConfig = new Config();
        }

        //Update config file in case it was missing to begin with or there are new/unused fields
        try {
            ConfigUtil.saveConfig(initializedConfig, configFilePathUsed);
        } catch (IOException e) {
            logger.warning("Failed to save config file : " + StringUtil.getDetails(e));
        }
        return initializedConfig;
    }

    /**
     * Returns a {@code UserPrefs} using the file at {@code storage}'s user prefs file path,
     * or a new {@code UserPrefs} with default configuration if errors occur when
     * reading from the file.
     */
    protected UserPrefs initPrefs(UserPrefsStorage storage) {
        Path prefsFilePath = storage.getUserPrefsFilePath();
        logger.info("Using prefs file : " + prefsFilePath);

        UserPrefs initializedPrefs;
        try {
            Optional<UserPrefs> prefsOptional = storage.readUserPrefs();
            initializedPrefs = prefsOptional.orElse(new UserPrefs());
        } catch (DataConversionException e) {
            logger.warning("UserPrefs file at " + prefsFilePath + " is not in the correct format. "
                    + "Using default user prefs");
            initializedPrefs = new UserPrefs();
        } catch (IOException e) {
            logger.warning("Problem while reading from the file. Will be starting with an empty FlashNotes");
            initializedPrefs = new UserPrefs();
        }

        //Update prefs file in case it was missing to begin with or there are new/unused fields
        try {
            storage.saveUserPrefs(initializedPrefs);
        } catch (IOException e) {
            logger.warning("Failed to save config file : " + StringUtil.getDetails(e));
        }

        return initializedPrefs;
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting FlashNotes " + MainApp.VERSION);
        ui.start(primaryStage);
    }

    @Override
    public void stop() {
        logger.info("============================ [ Stopping FlashNotes ] =============================");
        try {
            logger.info("Saving User Pref...");
            storage.saveUserPrefs(model.getUserPrefs());
            logger.info("Saving FlashNotes data...");
            storage.saveFlashNotes(model.getFlashNotes(), model.getUniqueDeckList());
        } catch (IOException e) {
            logger.severe("Failed to save preferences or data to file " + StringUtil.getDetails(e));
        }
        logger.info("============================ [ FlashNotes Exited ] =============================");
    }
}
