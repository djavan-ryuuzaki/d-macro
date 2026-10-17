package br.com.djavanfernando.dmacro.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Embedded
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "macros")
data class MacroEntity(
    @PrimaryKey val id: String,
    val nome: String,
    val ativada: Boolean,
    /** Macro completa em JSON ([br.com.djavanfernando.dmacro.engine.MacroJson]). */
    val json: String,
    val criadaEm: Long,
)

@Entity(tableName = "execucoes", indices = [Index("macroId"), Index("instante")])
data class ExecucaoEntity(
    @PrimaryKey val id: String,
    val macroId: String,
    val macroNome: String,
    val instante: Long,
    val teste: Boolean,
    val origemPacote: String?,
    val origemApp: String?,
    val origemTitulo: String?,
    val origemTexto: String?,
    /** Discriminador do desfecho, para consultas sem decodificar o JSON. */
    val tipoDesfecho: String,
    val desfechoJson: String,
)

@Entity(
    tableName = "execucao_acoes",
    foreignKeys = [ForeignKey(ExecucaoEntity::class, ["id"], ["execucaoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("execucaoId")],
)
data class ItemEntity(
    @PrimaryKey val id: String,
    val execucaoId: String,
    val posicao: Int,
    val indiceAcao: Int,
    val tipo: String,
    val destinatario: String,
    val rotulo: String?,
    val resultadoJson: String,
)

data class ExecucaoComItens(
    @Embedded val execucao: ExecucaoEntity,
    @Relation(parentColumn = "id", entityColumn = "execucaoId") val itens: List<ItemEntity>,
)

@Entity(tableName = "usos_teto", indices = [Index("tipo", "instante")])
data class UsoTetoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: String,
    val instante: Long,
)

@Dao
interface MacroDao {
    @Query("SELECT * FROM macros ORDER BY criadaEm")
    fun observar(): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macros WHERE ativada = 1 ORDER BY criadaEm")
    suspend fun ativadas(): List<MacroEntity>

    @Query("SELECT * FROM macros WHERE id = :id")
    suspend fun obter(id: String): MacroEntity?

    @Upsert
    suspend fun salvar(macro: MacroEntity)

    @Query("DELETE FROM macros WHERE id = :id")
    suspend fun excluir(id: String)
}

@Dao
interface ExecucaoDao {
    @Insert
    suspend fun inserirExecucao(execucao: ExecucaoEntity)

    @Insert
    suspend fun inserirItens(itens: List<ItemEntity>)

    @Transaction
    suspend fun inserir(execucao: ExecucaoEntity, itens: List<ItemEntity>, limiteInstante: Long, maximo: Int) {
        inserirExecucao(execucao)
        inserirItens(itens)
        apagarAntesDe(limiteInstante)
        manterUltimas(maximo)
    }

    @Query("UPDATE execucao_acoes SET resultadoJson = :resultadoJson WHERE id = :id")
    suspend fun atualizarItem(id: String, resultadoJson: String)

    @Query("UPDATE execucao_acoes SET resultadoJson = :para WHERE id = :id AND resultadoJson = :de")
    suspend fun substituirResultado(id: String, de: String, para: String)

    @Transaction
    @Query("SELECT * FROM execucoes ORDER BY instante DESC, rowid DESC")
    fun observar(): Flow<List<ExecucaoComItens>>

    @Transaction
    @Query("SELECT * FROM execucoes WHERE id = :id")
    fun observar(id: String): Flow<ExecucaoComItens?>

    @Query("SELECT MAX(instante) FROM execucoes WHERE macroId = :macroId AND teste = 0 AND tipoDesfecho = 'executada'")
    suspend fun ultimaExecucao(macroId: String): Long?

    @Query("DELETE FROM execucoes WHERE instante < :limite")
    suspend fun apagarAntesDe(limite: Long)

    @Query("DELETE FROM execucoes WHERE id NOT IN (SELECT id FROM execucoes ORDER BY instante DESC, rowid DESC LIMIT :maximo)")
    suspend fun manterUltimas(maximo: Int)

    @Query("DELETE FROM execucoes")
    suspend fun limpar()
}

@Dao
interface TetoDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(uso: UsoTetoEntity)

    @Query("SELECT COUNT(*) FROM usos_teto WHERE tipo = :tipo AND instante > :desde")
    suspend fun contar(tipo: String, desde: Long): Int

    @Query("DELETE FROM usos_teto WHERE instante < :limite")
    suspend fun apagarAntesDe(limite: Long)
}

@Database(
    entities = [MacroEntity::class, ExecucaoEntity::class, ItemEntity::class, UsoTetoEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DMacroDatabase : RoomDatabase() {
    abstract fun macroDao(): MacroDao
    abstract fun execucaoDao(): ExecucaoDao
    abstract fun tetoDao(): TetoDao
}
