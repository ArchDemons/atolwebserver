package ru.archdemon.atol.webserver;

public class Utils {

    public static String getStatusString(int status) {
        switch (status) {
            case Consts.STATUS_WAIT:
                return "wait";
            case Consts.STATUS_IN_PROGRESS:
                return "inProgress";
            case Consts.STATUS_READY:
                return "ready";
            case Consts.STATUS_ERROR:
                return "error";
            case Consts.STATUS_INTERRUPTED_BY_PREVIOUS_ERRORS:
                return "interrupted";
            case Consts.STATUS_BLOCKED:
                return "blocked";
            case Consts.STATUS_CANCELED:
                return "canceled";
        }
        return "unknown";
    }

    public static boolean isFiscalOperation(String operationType) {
        switch (operationType) {
            case "openShift":
            case "closeShift":
            case "sell":
            case "buy":
            case "sellReturn":
            case "buyReturn":
            case "sellCorrection":
            case "buyCorrection":
            case "reportOfdExchangeStatus":
            case "registration":
            case "fnChange":
            case "changeRegistrationParameters":
            case "closeArchive":
                return true;
        }
        return false;
    }

    public static boolean isReceipt(String operationType) {
        switch (operationType) {
            case "sell":
            case "buy":
            case "sellReturn":
            case "buyReturn":
            case "sellCorrection":
            case "buyCorrection":
                return true;
        }
        return false;
    }

}
