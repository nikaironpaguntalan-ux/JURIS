public class CaseRec {

    public enum CaseStatus {
        Active,Resolved,Dismissed
    }

    private int dbId;
    private String caseID;
    private String caseType;
    private String caseNature;
    private String caseStatus;
    private String accused;
    private String complainant;
    private String filedDate;
    private String caseDesc;

    private String prosecutor;
    private String judge;
    private String hearingDate;
    private String witness;
    private String evidence;
    private String branch;
    private String verdict;

    public CaseRec(String caseID, String caseType, String caseNature, String caseStatus, String accused, String complainant, String prosecutor, String judge,String filedDate, String hearingDate,String witness, String evidence,String branch, String verdict, String caseDesc) {
        this.caseID=caseID;
        this.caseType=caseType;
        this.caseNature=caseNature;
        this.caseStatus=caseStatus;
        this.accused=accused;
        this.complainant = complainant;
        this.prosecutor= prosecutor;
        this.judge= judge;
        this.filedDate = filedDate;
        this.hearingDate = hearingDate;
        this.witness= witness;
        this.evidence =evidence;
        this.branch = branch;
        this.verdict= verdict;
        this.caseDesc = caseDesc;
    }

    public int getDbId(){
        return dbId; 
    }

    public String getCaseID(){
        return caseID;
    }

    public String getCaseType(){
        return caseType;
    }

    public String getCaseNature(){
        return caseNature;
    }

    public String getCaseStatus(){
        return caseStatus;
    }

    public String getAccused(){
        return accused;
    }

    public String getComplainant(){
        return complainant;
    }

    public String getProsecutor(){
        return prosecutor;
    }

    public String getJudge() {
        return judge;
    }

    public String getFiledDate(){
        return filedDate;
    }

    public String getHearingDate(){ 
        return hearingDate;
    }

    public String getWitness() {
        return witness;
    }

    public String getEvidence(){
        return evidence;
    }

    public String getBranch(){
        return branch;
    }

    public String getVerdict(){
        return verdict;
    }

    public String getCaseDesc() {
        return caseDesc;
    }



    public void setDbId(int dbId){
        this.dbId = dbId;
    }

    public void setCaseID(String caseID){
        this.caseID = caseID;
    }

    public void setCaseType(String caseType){
        this.caseType = caseType;
    }

    public void setCaseNature(String caseNature){
        this.caseNature = caseNature; 
    }

    public void setCaseStatus(String caseStatus) {
        this.caseStatus = caseStatus;
    }

    public void setAccused(String accused){ 
        this.accused = accused; 
    }

    public void setComplainant(String complainant){ 
        this.complainant = complainant; 
    }

    public void setProsecutor(String prosecutor) { 
        this.prosecutor = prosecutor; 
    }

    public void setJudge(String judge){
        this.judge = judge; 
    }

    public void setFiledDate(String filedDate){
        this.filedDate = filedDate;
    }

    public void setHearingDate(String hearingDate){
        this.hearingDate = hearingDate;
    }

    public void setWitness(String witness){
        this.witness = witness;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence; 
    }

    public void setBranch(String branch){ 
        this.branch = branch; 
    }

    public void setVerdict(String verdict){ 
        this.verdict = verdict; 
    }

    public void setCaseDesc(String caseDesc){
        this.caseDesc = caseDesc;
    }

    @Override
    public String toString() {
        return "Case ID: "+ caseID
             + "\nType: "+caseType
             + "\nNature: "+caseNature
             + "\nStatus: "+caseStatus
             + "\nAccused: "+accused
             + "\nComplainant: "+ complainant
             + "\nFiled: "+ filedDate;
    }
}
