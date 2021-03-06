
package br.com.crescer.contra.cheque.service;

import br.com.crescer.contra.cheque.entity.Colaborador;
import br.com.crescer.contra.cheque.entity.Log;
import br.com.crescer.contra.cheque.entity.enumeration.TipoOperacaoLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import br.com.crescer.contra.cheque.repository.LogRepository;

/**
 *
 * @author otavio.bubans
 */
@Service
public class LogService {
    
    @Autowired
    LogRepository logRepository;
    
    public Log save(Log log){
        return logRepository.save(log);
    }
    
    public Long findByIdColaboradorAndTipoOperacaoAndIp(Colaborador colaborador, TipoOperacaoLog tipoOperacao, String id){
        return logRepository.countByIdColaboradorAndTipoOperacaoAndIp(colaborador, tipoOperacao, id);
    }
    
    public Long findByIdColaboradorAndTipoOperacao(Colaborador idColaborador, TipoOperacaoLog tipoOperacao){
        return logRepository.countByIdColaboradorAndTipoOperacao(idColaborador, tipoOperacao);
    }
}
