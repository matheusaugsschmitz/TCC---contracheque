package br.com.crescer.contra.cheque.web;

import br.com.crescer.contra.cheque.entity.Acesso;
import br.com.crescer.contra.cheque.entity.Colaborador;
import br.com.crescer.contra.cheque.entity.Email;
import br.com.crescer.contra.cheque.entity.Lancamento;
import br.com.crescer.contra.cheque.entity.Log;
import br.com.crescer.contra.cheque.service.UsuarioService;
import br.com.crescer.contra.cheque.entity.Usuario;
import br.com.crescer.contra.cheque.entity.enumeration.TipoOperacaoLog;
import br.com.crescer.contra.cheque.service.AcessoService;
import br.com.crescer.contra.cheque.service.DateService;
import br.com.crescer.contra.cheque.service.EmailService;
import br.com.crescer.contra.cheque.service.LancamentoService;
import br.com.crescer.contra.cheque.service.LogService;
import br.com.crescer.contra.cheque.service.exceptions.RegraDeNegocioException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 *
 * @author matha
 */
@Controller
public class HomeController {

    @Autowired
    UsuarioService usuarioService;

    @Autowired
    LogService logService;

    @Autowired
    AcessoService acessoService;

    @Autowired
    EmailService emailService;

    @Autowired
    HttpServletRequest request;

    @Autowired
    LancamentoService lancamentoService;

    @Autowired
    Environment environment;

    @Autowired
    DateService dateService;

    @Secured({"ROLE_USER"})
    @RequestMapping("/home")
    String home(Model model) {
        registrarAcesso();
        String role = usuarioLogado().getFuncao();
        model.addAttribute("anos", dateService.popularAnosAdmin());
        model.addAttribute("meses", dateService.popularMeses());
        if (role.equals("admin")) {
            return "admin";
        } else {
            return "home";
        }
    }

    @Secured({"ROLE_USER"})
    @RequestMapping(value = "/")
    String index() {
        return "apresentacao";
    }

    @Secured({"ROLE_ADMIN"})
    @RequestMapping(value = "/relatorio")
    String relatorio(Model model) {
        model.addAttribute("anos", dateService.popularAnosAdmin());
        model.addAttribute("meses", dateService.popularMeses());
        return "relatorio";
    }

    @Secured({"ROLE_ADMIN"})
    @RequestMapping(value = "/relatorio", method = RequestMethod.POST)
    String relatorio(Model model, String mes, Long ano, RedirectAttributes redirectAttributes) throws RegraDeNegocioException {
        Date data = new Date();

        data = dateService.DataSelecionada(mes, ano);

        double totalBeneficios = 0.0;
        double totalDescontos = 0.0;
        List<Lancamento> proventos = lancamentoService.pesquisarPorMesETipo(data, 'P');
        List<Lancamento> descontos = lancamentoService.pesquisarPorMesETipo(data, 'D');
        for (Lancamento desconto : descontos) {
            totalDescontos += desconto.getTotal();
        }
        for (Lancamento provento : proventos) {
            totalBeneficios += provento.getTotal();
        }
        model.addAttribute("anos", dateService.popularAnosAdmin());
        model.addAttribute("meses", dateService.popularMeses());
        model.addAttribute("totalBeneficios", String.format("R$ %1$,.2f", totalBeneficios));
        model.addAttribute("totalDescontos", String.format("R$ %1$,.2f", totalDescontos));
        return "relatorio";
    }

    @Secured({"ROLE_USER"})
    @RequestMapping(value = "/home", method = RequestMethod.POST)
    String home(Model model, String mes, Long ano, RedirectAttributes redirectAttributes) throws RegraDeNegocioException {
        Date dataPesquisada = dateService.DataSelecionada(mes, ano);
        Colaborador colaborador = usuarioLogado().getColaborador();
        List<Lancamento> listaDescontos = lancamentoService.pesquisarPorUsuarioMesETipo(colaborador, dataPesquisada, 'D');
        List<Lancamento> listaProventos = lancamentoService.pesquisarPorUsuarioMesETipo(colaborador, dataPesquisada, 'P');
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy");
        if (listaDescontos.isEmpty() && listaProventos.isEmpty()) {
            redirectAttributes.addFlashAttribute("msg", "Não foram encontrados registros nessa consulta");
            return "redirect: home";
        }
        model.addAttribute("usuario", usuarioLogado().getColaborador());
        model.addAttribute("admissao", formato.format(usuarioLogado().getColaborador().getAdminssao()));
        model.addAttribute("descontos", listaDescontos);
        model.addAttribute("proventos", listaProventos);
        model.addAttribute("totalLiquido", lancamentoService.pesquisarPorUsuarioMesECodigo(colaborador, dataPesquisada, "913"));
        model.addAttribute("irrf", lancamentoService.pesquisarPorUsuarioMesECodigo(colaborador, dataPesquisada, "711"));
        model.addAttribute("salarioBase", lancamentoService.pesquisarPorUsuarioMesECodigo(colaborador, dataPesquisada, "900"));
        model.addAttribute("inss", lancamentoService.pesquisarPorUsuarioMesECodigo(colaborador, dataPesquisada, "901"));
        model.addAttribute("fgts", lancamentoService.pesquisarPorUsuarioMesECodigo(colaborador, dataPesquisada, "902"));

        return "contracheque";
    }

    @Secured({"ROLE_ADMIN"})
    @RequestMapping(value = "/admin", method = RequestMethod.POST)
    String admin(@RequestParam("file") MultipartFile file, String mes, Long ano, RedirectAttributes redirectAttributes) {
        String nomeArquivo = file.getOriginalFilename();
        String diretorio = environment.getProperty("upload.arquivo");
        Path path = Paths.get(diretorio, nomeArquivo);
        Stream<String> arquivoImportado = null;
        Date dataDeImportacao = new Date();

        if (file.isEmpty() || file.getSize() == 0) {
            redirectAttributes.addFlashAttribute("msg", "O arquivo importado está vazio");
            return "redirect:home";
        }
        if (!nomeArquivo.contains(".txt")) {
            redirectAttributes.addFlashAttribute("msg", "O arquivo não está no formato .txt");
            return "redirect:home";
        }
        try (BufferedOutputStream buffer = new BufferedOutputStream(new FileOutputStream(new File(path.toString())))) {
            buffer.write(file.getBytes());
            arquivoImportado = Files.lines(path);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("msg", "Ocorreu um erro na leitura ou na importação do arquivo, tente novamente");
            return "redirect:home";
        }

        try {
            dataDeImportacao = dateService.DataSelecionada(mes, ano);
            if (dataDeImportacao.equals(new Date())) {
                redirectAttributes.addFlashAttribute("msg", "Ocorreu um erro na data informada, tente novamente");
                return "redirect:home";
            }
            lancamentoService.importarArquivo(arquivoImportado, dataDeImportacao);
        } catch (RegraDeNegocioException ex) {
            redirectAttributes.addFlashAttribute("msg", ex.getMessage());
            return "redirect:home";
        }
        registrarOperacao(usuarioLogado().getColaborador(), TipoOperacaoLog.IMPORTACAO, null);
        redirectAttributes.addFlashAttribute("success", "Arquivo importado com sucesso");

        return "redirect:home";
    }

    private void registrarAcesso() {
        Colaborador colaboradorLogado = usuarioLogado().getColaborador();
        Calendar c = new GregorianCalendar();
        c.setTime(new Date());
        List<String> diasSemana = Arrays.asList("domingo", "segunda-feira", "terca-feira", "quarta-feira", "quinta-feira", "sexta-feira", "sabado");
        int dia = c.get(Calendar.DAY_OF_WEEK);
        String diaSemana = diasSemana.get(dia);
        int hora = c.get(Calendar.HOUR_OF_DAY);
        Acesso acessoAtual = acessoService.findByIdColaboradorAndDiaSemanaAndHora(colaboradorLogado, diaSemana, hora);
        if (acessoAtual == null) {
            acessoAtual = new Acesso();
            acessoAtual.setDiaSemana(diaSemana);
            acessoAtual.setHora(hora);
            acessoAtual.setIdColaborador(colaboradorLogado);
            acessoAtual.setQtdAcessos(1);
        } else {
            acessoAtual.setQtdAcessos(acessoAtual.getQtdAcessos() + 1);
        }
        acessoService.save(acessoAtual);
        registrarOperacao(colaboradorLogado, TipoOperacaoLog.ACESSO, null);
        verificarIpSuspeito(colaboradorLogado);
    }

    private void registrarOperacao(Colaborador colaboradorLogado, TipoOperacaoLog tipoOperacao, Date dataConsultada) {
        String ipLogado = pegarIpLogado();
        Log log = new Log();
        log.setDataHora(new Date());
        log.setIdColaborador(colaboradorLogado);
        log.setIp(ipLogado);
        log.setTipoOperacao(tipoOperacao);
        log.setDataConsultaCc(dataConsultada);
        if (dataConsultada != null && tipoOperacao != TipoOperacaoLog.CONSULTA_CC) {
            log.setDataConsultaCc(null);
        }
        logService.save(log);
    }

    private void verificarIpSuspeito(Colaborador colaboradorLogado) {
        String ipLogado = pegarIpLogado();
        Long registros = logService.findByIdColaboradorAndTipoOperacao(colaboradorLogado, TipoOperacaoLog.ACESSO);
        Long registrosPorIp = logService.findByIdColaboradorAndTipoOperacaoAndIp(colaboradorLogado, TipoOperacaoLog.ACESSO, ipLogado);
        if (registros >= 10 && (ipLogado.equals("unknown") || registrosPorIp == 1)) {
            adicionarInvalidez(ipLogado);
        }
        verificarAcessoSuspeito(colaboradorLogado, ipLogado);
    }

    private void verificarAcessoSuspeito(Colaborador colaboradorLogado, String ipLogado) {
        List<Acesso> acessos = colaboradorLogado.getAcessos();
        Integer totalAcessos = 0;
        for (Acesso acesso : acessos) {
            totalAcessos += acesso.getQtdAcessos();
        }
        if (totalAcessos >= 10) {
            for (Acesso acesso : acessos) {
                int percentualAcesso = acesso.getQtdAcessos() / totalAcessos;
                if (percentualAcesso < 0.15) {
                    adicionarInvalidez(ipLogado);
                }
            };
        }

    }

    private void adicionarInvalidez(String ip) {
        Usuario usuarioLogado = usuarioLogado();
        int loginsSuspeitos = usuarioLogado.getLoginsSuspeitos();
        usuarioLogado.setLoginsSuspeitos(loginsSuspeitos + 1);
        if (loginsSuspeitos == 1) {
            Email novoEmail = new Email(usuarioLogado.getEmail(), ip, usuarioLogado().getColaborador().getNome());
            emailService.enviarEmail(novoEmail);
        } else {
            usuarioService.save(usuarioLogado);
        }
    }

    private Usuario usuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return usuarioService.findByEmail(username);
    }

    private String pegarIpLogado() {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null) {
            ip = request.getHeader("X_FORWARDED_FOR");
            if (ip == null) {
                ip = request.getRemoteAddr();
            }
        }
        if (ip.equals("0:0:0:0:0:0:0:1")) {
            ip = "127.0.0.1";
        }
        return ip;
    }
}
