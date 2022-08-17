/*notesTable` и `commentsTable`.
- В каждой таблице будет первичный ключ - `id`
- В commentsTable будет внешний (вторичный) ключ - `noteID`, совпадающий с `id` таблицы `notesTable`
- Так же добавим стандартные методы выборки (что-то типа заменителя запроса к БД) :
`getNotesList()` и `getСоmmentsList()
*/

/* <<<<<<<<<<<<<<<<<<<<<<<<<<< ИСКЛЮЧЕНИЯ: >>>>>>>>>>>>>>>>>>>>>>>>>>>>> */

class NotPermittedActionWithDeletedObject(message:String) : RuntimeException(message)

/* <<<<<<<<<<<<<<<<<<<<<<<<<<< ДАННЫЕ: >>>>>>>>>>>>>>>>>>>>>>>>>>>>> */

sealed interface Record { /* изолированный - наследника только два */
    val id: Long
    val userID: Long
    val text: String
    val parentID: Long?
    val isDeleted: Boolean
}

data class Note(
    override val id: Long = 0,
    override val userID: Long,
    override val text: String = "",
    override val parentID: Long? = null,
    override val isDeleted: Boolean = false
) : Record

data class Comment(
    override val id: Long = 0,
    override val userID: Long,
    override val text: String = "",
    override val parentID: Long?,
    override val isDeleted: Boolean = false
) : Record

data class User(
    val id: Long,
    val login: String,
    val password: String,
    val arrFriendsId: Array<Long>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (id != other.id) return false
        if (login != other.login) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + login.hashCode()
        return result
    }
}

/* <<<<<<<<<<<<<<<<<<<<<<<<<<< СЕРВИСЫ: >>>>>>>>>>>>>>>>>>>>>>>>>>>>> */

object Wall {

    /*  _______хранилища (аналоги таблиц)_______  */

    private var dbTableRecords: MutableSet<Record> = mutableSetOf()
    private var currentId: Long = 0


    /*  _______API_______           */

    // add:
    fun add(note:Note):Note { /* Создает новую заметку у текущего пользователя. */
        currentId += 1
        val newNote = note.copy(id = currentId)
        addRecord(newNote)
        return newNote
    }
    fun createComment(comment:Comment):Comment { /* Добавляет новый комментарий к заметке. */
        currentId += 1
        val newComment = comment.copy(id = currentId)
        addRecord(newComment)
        return newComment
    }

    // delete:
    fun delete(id:Long) { /* Удаляет заметку текущего пользователя. */
        deleteRecoverRecord(id, true)
    }
    fun deleteComment(id:Long) { /* Удаляет комментарий к заметке. */
        deleteRecoverRecord(id, true)
    }

    // restore:
    fun restoreComment(id:Long) { /* Восстанавливает удалённый комментарий. */
        deleteRecoverRecord(id, false)
    }
    fun recoverNote(id:Long) { /* Восстанавливает заметку */
        deleteRecoverRecord(id, false)
    }

    // edit:
    fun edit(id: Long, note:Note) { /* Редактирует заметку текущего пользователя. */
        editRecord(id, note)
    }
    fun editComment(id: Long, comment:Comment) { /* Редактирует указанный комментарий у заметки. */
        editRecord(id, comment)
    }

    // get:
    fun getNotes(userID:Long):Array<Note> { /* Возвращает список заметок, созданных пользователем. */
        var arrResult:Array<Note> = arrayOf()
        for (currRecord in dbTableRecords)  {
            if ((currRecord.userID == userID) && (currRecord is Note) && !currRecord.isDeleted) {
                arrResult += currRecord
            }
        }
        return arrResult
    }

    fun getById(id: Long):Record? { /* Возвращает заметку по её id. */
        for (currRecord in dbTableRecords)  {
            if (currRecord.id == id) {
                return currRecord
            }
        }
        return null
    }

    fun getComments(noteId: Long):Array<Comment> { /* Возвращает список комментариев к заметке. */
        var arrResult:Array<Comment> = arrayOf()
        for (currRecord in dbTableRecords)  {
            if ((currRecord.parentID == noteId) && (currRecord is Comment) && !currRecord.isDeleted) {
                arrResult += currRecord
            }
        }
        return arrResult
    }

    fun getFriendsNotes(user: User):Array<Note> { /* Возвращает список заметок друзей пользователя. */
        var arrAllFriendsNotes = emptyArray<Note>()
        for ((index,friendId) in user.arrFriendsId.withIndex()) {
            val arrOneFriendNotes = getNotes(friendId)
            arrAllFriendsNotes = arrAllFriendsNotes + arrOneFriendNotes
        }
        return arrAllFriendsNotes
    }



    /*  _______Внутренние методы_______           */

    private fun addRecord(record:Record) {
        if (dbTableRecords.contains(record)) {
            editRecord(record.id, record)
        }

        val parentID = record.parentID
        if (parentID !== null) {
            val parent = getById(parentID)
            if (parent?.isDeleted == true) {
                throw NotPermittedActionWithDeletedObject("Операция недопустима, т.к. нельзя создавать дочерний для удаленного")
            }
        }

        dbTableRecords.add(record);
    }

    private fun deleteRecoverRecord(id:Long, isDeleted:Boolean) { /* а можно не теребонькать БД удалением и записью нового, а просто поменять внутри существующего dbTableRecords ? */
        val currRecord = getById(id)
        val newRecord = when (currRecord) { // почему-то нельзя if ((currRecord is Note) || (currRecord is Comment))
            is Note  -> {currRecord.copy(isDeleted = isDeleted)}
            is Comment -> {currRecord.copy(isDeleted = isDeleted)}
            else -> {null}
        }
        dbTableRecords.remove(currRecord)
        if (newRecord !== null) {
            dbTableRecords.add(newRecord)
        }
    }

    private fun editRecord(id: Long, newRecord:Record) { /* опять же, вместо удаления и записи - как изменить прямо в списке? */
        val currRecord = getById(id)
        if (currRecord !== null) {
            if (currRecord?.isDeleted == true) {
                throw NotPermittedActionWithDeletedObject("Операция недопустима, т.к. текущая запись удалена")
            }

            val parentID = currRecord.parentID
            if (parentID !== null) {
                val parent = getById(parentID)
                if (parent?.isDeleted == true) {
                    throw NotPermittedActionWithDeletedObject("Операция недопустима, т.к. родительская запись удалена")
                }
            }

            dbTableRecords.remove(currRecord)
            dbTableRecords.add(newRecord)
        }
    }

}

/* <<<<<<<<<<<<<<<<<<<<<<<<<<< main: >>>>>>>>>>>>>>>>>>>>>>>>>>>>> */

fun main() {
    val masha = User(1,"masha", "password", arrayOf(2,3))
    val petya = User(2,"petya", "$%FYdnfb4f", emptyArray())
    val vasya = User(3,"vasya", "sdfJFG3f^$", emptyArray())


    val mashasNote = Wall.add(Note(userID = masha.id, text = "Маша написала заметку"))
    val petyasNote = Wall.add(Note(userID = petya.id, text = "Петя написал заметку"))
    val vasyasNote = Wall.add(Note(userID = vasya.id, text = "Вася написал заметку"))
    Wall.createComment(Comment(userID = petya.id, text = "Петя написал комментарий Маше", parentID = mashasNote.id))
    println(Wall.getFriendsNotes(masha))

    Wall.delete(mashasNote.id)
    // ловим ошибку:
    Wall.createComment(Comment(userID = petya.id, text = "Петя пытается написать еще один комментарий Маше", parentID = mashasNote.id))

}