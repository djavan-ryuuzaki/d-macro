package br.com.djavanfernando.dmacro.di

import android.content.Context
import androidx.room.Room
import br.com.djavanfernando.dmacro.data.ConfiguracoesRepository
import br.com.djavanfernando.dmacro.data.DMacroDatabase
import br.com.djavanfernando.dmacro.data.MacroRepository
import br.com.djavanfernando.dmacro.data.RegistroRepository
import br.com.djavanfernando.dmacro.data.TetoRepository
import br.com.djavanfernando.dmacro.data.dataStore
import br.com.djavanfernando.dmacro.engine.Motor
import br.com.djavanfernando.dmacro.engine.whatsapp.FilaWhatsApp
import br.com.djavanfernando.dmacro.platform.AvisosAndroid
import br.com.djavanfernando.dmacro.platform.CatalogoApps
import br.com.djavanfernando.dmacro.platform.ChamadaAndroid
import br.com.djavanfernando.dmacro.platform.ContaEmailRepository
import br.com.djavanfernando.dmacro.platform.EmailWorkManager
import br.com.djavanfernando.dmacro.platform.EstadoAparelhoAndroid
import br.com.djavanfernando.dmacro.platform.PrazoWhatsAppWorkManager
import br.com.djavanfernando.dmacro.platform.TelaWhatsAppAndroid
import br.com.djavanfernando.dmacro.platform.PacotesVistos
import br.com.djavanfernando.dmacro.platform.PermissoesAndroid
import br.com.djavanfernando.dmacro.platform.SaudeApp
import br.com.djavanfernando.dmacro.platform.SmsAndroid
import br.com.djavanfernando.dmacro.platform.SmtpCliente
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Qualifier
import javax.inject.Singleton

/** Escopo do processo, para trabalho que não pertence a uma tela. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EscopoApp

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton @EscopoApp
    fun escopoApp(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton
    fun relogio(): Clock = Clock.systemDefaultZone()

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): DMacroDatabase =
        Room.databaseBuilder(context, DMacroDatabase::class.java, "dmacro.db").build()

    @Provides @Singleton
    fun macros(db: DMacroDatabase) = MacroRepository(db.macroDao())

    @Provides @Singleton
    fun registros(db: DMacroDatabase) = RegistroRepository(db.execucaoDao())

    @Provides @Singleton
    fun teto(db: DMacroDatabase) = TetoRepository(db.tetoDao())

    @Provides @Singleton
    fun configuracoes(@ApplicationContext context: Context) = ConfiguracoesRepository(context.dataStore)

    @Provides @Singleton
    fun permissoes(@ApplicationContext context: Context) = PermissoesAndroid(context)

    @Provides @Singleton
    fun contaEmail(@ApplicationContext context: Context) = ContaEmailRepository(context)

    @Provides @Singleton
    fun smtp() = SmtpCliente()

    @Provides @Singleton
    fun pacotesVistos(@ApplicationContext context: Context) = PacotesVistos(context)

    @Provides @Singleton
    fun catalogoApps(@ApplicationContext context: Context, vistos: PacotesVistos) = CatalogoApps(context, vistos)

    @Provides @Singleton
    fun saude(@ApplicationContext context: Context, permissoes: PermissoesAndroid, contas: ContaEmailRepository) =
        SaudeApp(context, permissoes, contas)

    @Provides @Singleton
    fun filaWhatsApp(
        @ApplicationContext context: Context,
        relogio: Clock,
        @EscopoApp escopo: CoroutineScope,
        registros: RegistroRepository,
    ) = FilaWhatsApp(
        escopo = escopo,
        relogio = relogio,
        aparelho = EstadoAparelhoAndroid(context),
        tela = TelaWhatsAppAndroid(context),
        prazos = PrazoWhatsAppWorkManager(context, relogio),
        log = registros,
    )

    @Provides @Singleton
    fun motor(
        @ApplicationContext context: Context,
        relogio: Clock,
        @EscopoApp escopo: CoroutineScope,
        macros: MacroRepository,
        configuracoes: ConfiguracoesRepository,
        permissoes: PermissoesAndroid,
        registros: RegistroRepository,
        teto: TetoRepository,
        contas: ContaEmailRepository,
        whatsapp: FilaWhatsApp,
    ) = Motor(
        pacoteProprio = context.packageName,
        relogio = relogio,
        escopo = escopo,
        macros = macros,
        configuracoes = configuracoes,
        permissoes = permissoes,
        sms = SmsAndroid(context),
        chamadas = ChamadaAndroid(context),
        email = EmailWorkManager(context, contas, relogio),
        whatsapp = whatsapp,
        log = registros,
        teto = teto,
        avisos = AvisosAndroid(context, permissoes),
    )
}
