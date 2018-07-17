package mutilthreaddemo

class BooksScanTime {
    Date createTime
    Date modifiedTime
    Date scanStartTime
    Date scanEndTime
    Date curDate
    boolean compared

    static constraints = {
        createTime nullable: true
        modifiedTime nullable: true
    }

    def beforeInsert = {
        createTime = new Date()
        modifiedTime = createTime
    }

    def beforeUpdate = {
        modifiedTime = new Date()
    }
}
