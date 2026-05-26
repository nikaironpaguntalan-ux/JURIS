public class Schedules {

    private int dbId;     
    private String datetime; 
    private String caseID;
    private String reSched;   
    private String reason;    
    private String hsID;  

    public Schedules(String datetime, String caseID, String reSched, String reason, String hsID) {
        this.datetime = datetime;
        this.caseID = caseID;
        this.reSched = reSched;
        this.reason = reason;
        this.hsID = hsID;
    }

    public int getDbId(){ 
        return dbId; 
    }

    public String getDatetime(){ 
        return datetime; 
    }

    public String getCaseID(){ 
        return caseID; 
    }

    public String getReSched(){ 
        return reSched; 
    }

    public String getReason(){ 
        return reason; 
    }

    public String getHsID(){ 
        return hsID; 
    }


    public void setDbId(int dbId){ 
        this.dbId = dbId; 
    }

    public void setDatetime(String datetime){ 
        this.datetime = datetime; 
    }

    public void setCaseID(String caseID){ 
        this.caseID = caseID; 
    }

    public void setReSched(String reSched){ 
        this.reSched = reSched; 
    }

    public void setReason(String reason){
        this.reason = reason; 
    }

    public void setHsID(String hsID){
        this.hsID = hsID; 
    }

    @Override
    public String toString() {
        return "Case " + caseID + " | Scheduled: " + datetime + 
        (reSched != null && !reSched.isEmpty() ? " | Rescheduled: " + reSched : "") + 
        (reason != null && !reason.isEmpty() ? " | Reason: " + reason : "");
    }
}
