package com.procurement.requisition.infrastructure.repository

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.HostDistance
import com.datastax.driver.core.PlainTextAuthProvider
import com.datastax.driver.core.PoolingOptions
import com.datastax.driver.core.Session
import com.nhaarman.mockito_kotlin.spy
import com.procurement.requisition.infrastructure.bind.date.JsonDateTimeDeserializer
import com.procurement.requisition.infrastructure.bind.date.JsonDateTimeSerializer
import com.procurement.requisition.infrastructure.handler.Action
import com.procurement.requisition.infrastructure.handler.CommandType
import com.procurement.requisition.infrastructure.service.HistoryEntity
import com.procurement.requisition.infrastructure.service.HistoryRepository
import com.procurement.requisition.infrastructure.web.dto.CommandId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime
import java.util.*

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [DatabaseTestConfiguration::class])
class CassandraHistoryRepositoryIT {

    companion object {
        private const val KEYSPACE = "requisition"
        private const val TABLE_NAME = "history"
        private const val COMMAND_ID_COLUMN = "command_id"
        private const val COMMAND_NAME_COLUMN = "command_name"
        private const val COMMAND_DATE_COLUMN = "command_date"
        private const val JSON_DATA_COLUMN = "json_data"

        private val COMMAND_ID: CommandId = UUID.randomUUID().toString()
        private val COMMAND_NAME: Action = CommandType.VALIDATE_PCR_DATA
        private val COMMAND_DATE =
            JsonDateTimeDeserializer.deserialize(JsonDateTimeSerializer.serialize(LocalDateTime.now()))
        private const val JSON_DATA: String = """{"tender": {"title" : "Tender-Title"}}"""

        private val HISTORY_ENTITY = HistoryEntity(
            commandId = COMMAND_ID,
            action = COMMAND_NAME,
            date = COMMAND_DATE,
            data = JSON_DATA,
        )
    }

    @Autowired
    private lateinit var container: CassandraTestContainer
    private lateinit var session: Session
    private lateinit var repository: HistoryRepository

    @BeforeEach
    fun init() {
        val poolingOptions = PoolingOptions()
            .setMaxConnectionsPerHost(HostDistance.LOCAL, 1)
        val cluster = Cluster.builder()
            .addContactPoints(container.contractPoint)
            .withPort(container.port)
            .withoutJMXReporting()
            .withPoolingOptions(poolingOptions)
            .withAuthProvider(PlainTextAuthProvider(container.username, container.password))
            .build()

        session = spy(cluster.connect())

        createKeyspace()
        createTable()

        repository = CassandraHistoryRepository(session)
    }

    @AfterEach
    fun clean() {
        dropKeyspace()
    }

    @Test
    fun saveHistory() {
        val result = repository.saveHistory(HISTORY_ENTITY)

        assertTrue(result.isSuccess)
        result.forEach {
            assertEquals(HISTORY_ENTITY.commandId, it.commandId)
            assertEquals(HISTORY_ENTITY.action, it.action)
            assertEquals(HISTORY_ENTITY.date, it.date)
            assertEquals(HISTORY_ENTITY.data, it.data)
        }
    }

    @Test
    fun getHistory() {

        val savedResult = repository.saveHistory(HISTORY_ENTITY)

        assertTrue(savedResult.isSuccess)
        savedResult.forEach {
            assertEquals(HISTORY_ENTITY.commandId, it.commandId)
            assertEquals(HISTORY_ENTITY.action, it.action)
            assertEquals(HISTORY_ENTITY.date, it.date)
            assertEquals(HISTORY_ENTITY.data, it.data)
        }

        val loadedResult = repository.getHistory(commandId = HISTORY_ENTITY.commandId)
        assertTrue(loadedResult.isSuccess)
        loadedResult.forEach {
            assertNotNull(it)
            assertEquals(HISTORY_ENTITY.commandId, it!!.commandId)
            assertEquals(HISTORY_ENTITY.action, it.action)
            assertEquals(HISTORY_ENTITY.date, it.date)
            assertEquals(HISTORY_ENTITY.data, it.data)
        }
    }

    private fun createKeyspace() {
        session.execute(
            "CREATE KEYSPACE $KEYSPACE " +
                "WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1};"
        )
    }

    private fun dropKeyspace() {
        session.execute("DROP KEYSPACE $KEYSPACE;")
    }

    private fun createTable() {
        session.execute(
            """
                CREATE TABLE IF NOT EXISTS $KEYSPACE.$TABLE_NAME
                    (
                        $COMMAND_ID_COLUMN   TEXT,
                        $COMMAND_NAME_COLUMN TEXT,
                        $COMMAND_DATE_COLUMN TIMESTAMP,
                        $JSON_DATA_COLUMN    TEXT,
                        PRIMARY KEY ($COMMAND_ID_COLUMN)
                    );
            """
        )
    }
}


