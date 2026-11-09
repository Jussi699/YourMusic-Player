package yourmusic.utility;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import yourmusic.logger.ErrorLogger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class AppSettingsStore {
    private static final String FILE_NAME = "settings.xml";

    private AppSettingsStore() {
    }

    public static synchronized AppSettings loadOrCreate() throws IOException {
        Path file = settingsFile();
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) {
            try (InputStream defaults = AppSettingsStore.class.getResourceAsStream("/settings-default.xml")) {
                if (defaults == null) {
                    throw new IOException("Bundled default settings file is missing");
                }
                Files.copy(defaults, file);
            }
        }
        try {
            return read(file);
        } catch (Exception e) {
            throw new IOException("Could not read settings file: " + file, e);
        }
    }

    public static synchronized void save(AppSettings settings) throws IOException {
        Path file = settingsFile();
        try {
            Files.createDirectories(file.getParent());
            Document document = createDocument();
            Element root = document.createElement("settings");
            root.setAttribute("folderRandom", Boolean.toString(settings.folderRandom()));
            document.appendChild(root);
            Element volume = document.createElement("volume");
            volume.setTextContent(String.format(Locale.ROOT, "%.2f", settings.volume()));
            root.appendChild(volume);
            Element hotkeys = document.createElement("hotkeys");
            root.appendChild(hotkeys);
            for (Map.Entry<AppSettings.HotkeyAction, AppSettings.HotkeyBinding> entry : settings.hotkeys().entrySet()) {
                Element hotkey = document.createElement("hotkey");
                hotkey.setAttribute("action", entry.getKey().name());
                hotkey.setAttribute("key", entry.getValue().keyCode());
                hotkey.setAttribute("control", Boolean.toString(entry.getValue().control()));
                hotkey.setAttribute("alt", Boolean.toString(entry.getValue().alt()));
                hotkey.setAttribute("shift", Boolean.toString(entry.getValue().shift()));
                hotkeys.appendChild(hotkey);
            }

            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            var transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            try (OutputStream output = Files.newOutputStream(file)) {
                transformer.transform(new DOMSource(document), new StreamResult(output));
            }
        } catch (Exception e) {
            throw new IOException("Could not write settings file: " + file, e);
        }
    }

    public static synchronized void saveVolume(double volume) throws IOException {
        AppSettings current = loadOrCreate();
        save(new AppSettings(current.folderRandom(), current.hotkeys(), volume));
    }

    private static AppSettings read(Path file) throws Exception {
        Document document;
        try (InputStream input = Files.newInputStream(file)) {
            var builder = documentBuilderFactory().newDocumentBuilder();
            document = builder.parse(input);
        }

        Element root = document.getDocumentElement();
        boolean folderRandom = Boolean.parseBoolean(root.getAttribute("folderRandom"));
        NodeList volumeNodes = root.getElementsByTagName("volume");
        double volume = volumeNodes.getLength() > 0
                ? parseVolume(volumeNodes.item(0).getTextContent())
                : parseVolume(Info.readLegacyVolume());
        EnumMap<AppSettings.HotkeyAction, AppSettings.HotkeyBinding> bindings =
                new EnumMap<>(AppSettings.HotkeyAction.class);
        NodeList nodes = root.getElementsByTagName("hotkey");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element hotkey)) {
                continue;
            }
            try {
                AppSettings.HotkeyAction action = AppSettings.HotkeyAction.valueOf(hotkey.getAttribute("action"));
                AppSettings.HotkeyBinding binding = new AppSettings.HotkeyBinding(
                        hotkey.getAttribute("key"),
                        Boolean.parseBoolean(hotkey.getAttribute("control")),
                        Boolean.parseBoolean(hotkey.getAttribute("alt")),
                        Boolean.parseBoolean(hotkey.getAttribute("shift")));
                bindings.put(action, binding);
            } catch (IllegalArgumentException e) {
                ErrorLogger.log(309, ErrorLogger.Level.WARN, "Ignoring invalid hotkey in settings.xml: " + e.getMessage());
            }
        }
        AppSettings settings = new AppSettings(folderRandom, bindings, volume);
        if (volumeNodes.getLength() == 0) {
            save(settings);
        }
        return settings;
    }

    private static double parseVolume(String value) {
        if (value == null || value.isBlank()) {
            return AppSettings.defaults().volume();
        }
        try {
            double volume = Double.parseDouble(value);
            return Double.isFinite(volume) ? Math.clamp(volume, 0.0, 100.0) : 10.0;
        } catch (NumberFormatException _) {
            ErrorLogger.log(310, ErrorLogger.Level.WARN, "Ignoring invalid volume in settings: " + value);
            return 10.0;
        }
    }

    private static Document createDocument() throws Exception {
        return documentBuilderFactory().newDocumentBuilder().newDocument();
    }

    private static DocumentBuilderFactory documentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    public static Path settingsFile() {
        return LibraryStore.dataDirectory().resolve(FILE_NAME);
    }
}
