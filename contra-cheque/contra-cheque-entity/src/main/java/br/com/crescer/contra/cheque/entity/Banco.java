
package br.com.crescer.contra.cheque.entity;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import static javax.persistence.GenerationType.SEQUENCE;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author matha
 */
@Entity
@Table(name = "BANCO")
@XmlRootElement
public class Banco implements Serializable {

    private static final String SQ_NAME = "SEQ_BANCO";

    @Id
    @GeneratedValue(strategy = SEQUENCE, generator = SQ_NAME)
    @SequenceGenerator(name = SQ_NAME, sequenceName = SQ_NAME, allocationSize = 1)
    @Column(name = "ID_BANCO")
    private Long idBanco;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 40)
    @Column(name = "NOME")
    private String nome;
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "idBanco")
    private List<Agencia> agencias;

    public Banco() {
    }

    public Banco(String nome) {
        this.nome = nome;
    }

    public Long getIdBanco() {
        return idBanco;
    }

    public void setIdBanco(Long idBanco) {
        this.idBanco = idBanco;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    @XmlTransient
    public List<Agencia> getAgencias() {
        return agencias;
    }

    public void setAgencias(List<Agencia> agencias) {
        this.agencias = agencias;
    }

    public boolean equals(Banco banco) {
        if (this == banco) {
            return true;
        }
        if (banco == null) {
            return false;
        }
        if (!this.nome.equals(banco.nome)) {
            return false;
        }
        return this.idBanco.equals(banco.idBanco);
    }
    
}
