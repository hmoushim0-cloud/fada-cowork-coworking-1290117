package sa.factory.fadacoworkcoworkin.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import sa.factory.fadacoworkcoworkin.model.FadaCoworkItem

@Entity(tableName = "coworking_items_cache")
data class FadaCoworkEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priceSar: Double,
    val badge: String,
    val category: String,
    val status: String
) {
    fun toDomain(): FadaCoworkItem = FadaCoworkItem(
        id = id,
        title = title,
        description = description,
        priceSar = priceSar,
        badge = badge,
        category = category,
        status = status
    )
}
