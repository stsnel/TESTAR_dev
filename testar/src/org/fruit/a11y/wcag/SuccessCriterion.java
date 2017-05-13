/*****************************************************************************************
 *                                                                                       *
 * COPYRIGHT (2017):                                                                     *
 * Universitat Politecnica de Valencia                                                   *
 * Camino de Vera, s/n                                                                   *
 * 46022 Valencia, Spain                                                                 *
 * www.upv.es                                                                            *
 *                                                                                       * 
 * D I S C L A I M E R:                                                                  *
 * This software has been developed by the Universitat Politecnica de Valencia (UPV)     *
 * in the context of the TESTAR Proof of Concept project:                                *
 *               "UPV, Programa de Prueba de Concepto 2014, SP20141402"                  *
 * This sample is distributed FREE of charge under the TESTAR license, as an open        *
 * source project under the BSD3 licence (http://opensource.org/licenses/BSD-3-Clause)   *                                                                                        * 
 *                                                                                       *
 *****************************************************************************************/

package org.fruit.a11y.wcag;

/**
 * A WCAG success criterion
 * @author Davy Kager
 *
 */
public class SuccessCriterion extends ItemBase {
	
	private final AbstractGuideline parent;
	private final Level level;
	
	SuccessCriterion(int nr, String name, AbstractGuideline parent, Level level) {
		super(nr, name);
		this.parent = parent;
		this.level = level;
	}
	
	@Override
	public String getNr() {
		return parent.getNr() + "." + nr;
	}

	public Level getLevel() {
		return level;
	}
	
}
