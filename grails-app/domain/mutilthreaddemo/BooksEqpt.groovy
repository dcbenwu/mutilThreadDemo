package mutilthreaddemo

class BooksEqpt {
    String ip
    String device_type // product, test equipment ...
    String type
    String pn
    String sn
    String location
    Date search_date
    String state
    int serial

    Date createTime
    Date lastModifyTime

    static constraints = {
        device_type nullable: true
        state nullable: true
        createTime nullable: true
        lastModifyTime nullable: true
    }

    def beforeInsert() {
        createTime = new Date()
        lastModifyTime = createTime
    }

    def beforeUpdate() {
        lastModifyTime = new Date()
    }
}
