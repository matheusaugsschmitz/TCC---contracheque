/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.crescer.contra.cheque.service.exceptions;

/**
 *
 * @author mateus.teixeira
 */
public class RegraDeNegocioException extends Exception {
    
    public RegraDeNegocioException(String mensagem){
        super(mensagem);
    }
}