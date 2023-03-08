package material.hunter.models;

public class ServicesModel {

    private String ServiceName;
    private String CommandForStartingService;
    private String CommandForStoppingService;
    private String CommandForCheckingService;
    private String RunOnChrootStart;
    private String Status;

    public ServicesModel(
            String ServiceName,
            String CommandForStartingService,
            String CommandForStoppingService,
            String CommandForCheckingService,
            String RunOnChrootStart,
            String Status) {
        this.ServiceName = ServiceName;
        this.CommandForStartingService = CommandForStartingService;
        this.CommandForStoppingService = CommandForStoppingService;
        this.CommandForCheckingService = CommandForCheckingService;
        this.RunOnChrootStart = RunOnChrootStart;
        this.Status = Status;
    }

    public String getServiceName() {
        return ServiceName;
    }

    public void setServiceName(String ServiceName) {
        this.ServiceName = ServiceName;
    }

    public String getCommandForStartingService() {
        return CommandForStartingService;
    }

    public void setCommandForStartingService(String CommandForStartingService) {
        this.CommandForStartingService = CommandForStartingService;
    }

    public String getCommandForStoppingService() {
        return CommandForStoppingService;
    }

    public void setCommandForStoppingService(String CommandForStoppingService) {
        this.CommandForStoppingService = CommandForStoppingService;
    }

    public String getCommandForCheckingService() {
        return CommandForCheckingService;
    }

    public void setCommandForCheckingService(String CommandforCheckServiceStatus) {
        this.CommandForCheckingService = CommandforCheckServiceStatus;
    }

    public String getRunOnChrootStart() {
        return RunOnChrootStart;
    }

    public void setRunOnChrootStart(String RunOnChrootStart) {
        this.RunOnChrootStart = RunOnChrootStart;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String Status) {
        this.Status = Status;
    }
}