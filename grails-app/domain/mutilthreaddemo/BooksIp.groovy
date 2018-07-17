package mutilthreaddemo

class BooksIp {

    String ip
    Date createTime
    Date modifyTime
    String state      // REPT EVT SESSION; REPT DBCHG

    static constraints = {
        createTime nullable: true
        modifyTime nullable: true
        state nullable: true
    }

    def beforeInsert = {
        createTime = new Date()
        modifyTime = createTime
    }

    def beforeUpdate = {
        modifyTime = new Date()
    }
}
