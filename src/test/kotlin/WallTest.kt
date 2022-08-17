import org.junit.Test

import org.junit.Assert.*

class WallTest {

    @Test
    fun add_getRightId() {
        val masha = User(1,"masha", "password", emptyArray())
        val mashasNote = Wall.add(Note(userID = masha.id, text = "Маша написала заметку"))
        assertEquals(1, mashasNote.id)
    }

    @Test (expected = NotPermittedActionWithDeletedObject::class)
    fun createComment_ThrowEx_DeletedParent() {
        val masha = User(1,"masha", "password", emptyArray())
        val mashasNote = Wall.add(Note(userID = masha.id, text = "Маша написала заметку"))
        Wall.delete(mashasNote.id)
        // ловим ошибку:
        Wall.createComment(Comment(userID = 2, text = "Кто-то пытается написать комментарий к уже удаленному посту", parentID = mashasNote.id))
    }
}