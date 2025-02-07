package ru.archdemon.atol.webserver.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.json.simple.JSONObject;
import ru.atol.drivers10.fptr.IFptr;

@Data
@EqualsAndHashCode(of = "id")
public class Device {

    private String id;
    private String name;
    private boolean isActive;
    private boolean isDefault;
    private int model;
    private String userPassword;
    private String accessPassword;
    private String port;
    private String com;
    private String usb;
    private int baudRate;
    private String ipAddr;
    private int ipPort;
    private String mac;
    private String ofdChannel;
    private boolean useGlobalSp;
    private String scriptsPath;
    private boolean useGlobalIcds;
    private boolean invertCdStatus;
    private boolean useGlobalHl;
    private String headerLines;
    private boolean useGlobalFl;
    private String footerLines;
    private long blockId;

    public Device(String id) {
        this.id = id;
    }

    public String getSettings() {
        JSONObject result = new JSONObject();
        result.put(IFptr.LIBFPTR_SETTING_MODEL, model);
        result.put(IFptr.LIBFPTR_SETTING_PORT, port);
        result.put(IFptr.LIBFPTR_SETTING_COM_FILE, com);
        result.put(IFptr.LIBFPTR_SETTING_BAUDRATE, baudRate);
        result.put(IFptr.LIBFPTR_SETTING_USB_DEVICE_PATH, usb);
        result.put(IFptr.LIBFPTR_SETTING_OFD_CHANNEL, ofdChannel);
        result.put(IFptr.LIBFPTR_SETTING_IPADDRESS, ipAddr);
        result.put(IFptr.LIBFPTR_SETTING_IPPORT, ipPort);
        result.put(IFptr.LIBFPTR_SETTING_ACCESS_PASSWORD, accessPassword);
        result.put(IFptr.LIBFPTR_SETTING_USER_PASSWORD, userPassword);

        return result.toJSONString();
    }

    public JSONObject toJson() {
        JSONObject connection = new JSONObject();
        connection.put("accessPassword", accessPassword);
        connection.put("baudRate", baudRate);
        connection.put("com", com);
        connection.put("ipAddress", ipAddr);
        connection.put("ipPort", ipPort);
        connection.put("mac", mac);
        connection.put("model", model);
        connection.put("ofdChannel", ofdChannel);
        connection.put("port", port);
        connection.put("usbDevice", usb);
        connection.put("userPassword", userPassword);

        JSONObject other = new JSONObject();
        other.put("additionalFooterLines", footerLines);
        other.put("additionalHeaderLines", headerLines);
        other.put("invertCashDrawerStatus", invertCdStatus);
        other.put("scriptsPath", scriptsPath);
        other.put("useGlobalAdditionalFooterLines", useGlobalFl);
        other.put("useGlobalAdditionalHeaderLines", useGlobalHl);
        other.put("useGlobalInvertCashDrawerStatusFlag", useGlobalIcds);
        other.put("useGlobalScriptsSettings", useGlobalSp);

        JSONObject result = new JSONObject();
        result.put("connectionSettings", connection);
        result.put("id", id);
        result.put("isActive", isActive);
        result.put("isDefault", isDefault);
        result.put("name", name);
        result.put("otherSettings", other);

        return result;
    }

}
