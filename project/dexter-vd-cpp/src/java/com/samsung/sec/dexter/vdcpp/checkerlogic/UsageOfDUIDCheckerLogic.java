/**
 *  @file   CheckUsageOfDUID.java
 *  @brief  CheckUsageOfDUID class source file
 *  @author adarsh.t
 *
 * Copyright 2015 by Samsung Electronics, Inc.
 * All rights reserved.
 * 
 * Project Description :
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung Electronics.
 */

package com.samsung.sec.dexter.vdcpp.checkerlogic;

import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTEqualsInitializer;

import com.samsung.sec.dexter.core.analyzer.AnalysisConfig;
import com.samsung.sec.dexter.core.analyzer.AnalysisResult;
import com.samsung.sec.dexter.core.checker.Checker;
import com.samsung.sec.dexter.core.defect.PreOccurence;
import com.samsung.sec.dexter.vdcpp.plugin.DexterVdCppPlugin;
import com.samsung.sec.dexter.vdcpp.util.CppUtil;


public class UsageOfDUIDCheckerLogic implements ICheckerLogic
{
	private IASTTranslationUnit translationUnit;	
	private String[] matchString =null;
	private String matchFunction ="vconf_get_str";
	@Override
	public void analyze(final AnalysisConfig config, final AnalysisResult result, 
			final Checker checker, IASTTranslationUnit unit) {
		translationUnit =unit;
		matchString= checker.getProperty("id-list").split(",");
		ASTVisitor visitor = createVisitor(config, result, checker);
		visitor.shouldVisitDeclarations = true;
		unit.accept(visitor);
	}

	private ASTVisitor createVisitor(final AnalysisConfig config,
			final AnalysisResult result, final Checker checker) {
		ASTVisitor visitor = new ASTVisitor() {
			@Override
			public int visit(IASTDeclaration ast ) {

				if(ast instanceof IASTFunctionDefinition)
				{									
					visitFunction(config, result, checker, ast);
				}		
				
				return super.visit(ast);
			}
			
			private void visitFunction(final AnalysisConfig config,
					final AnalysisResult result, final Checker checker,
					IASTDeclaration ast) {
				ASTVisitor visitor = new ASTVisitor() {
					public int visit(IASTExpression astExpression ) {							

						if(astExpression instanceof IASTFunctionCallExpression)
						{				
							visitFunctionCallExpression(config, result,
									checker, astExpression);

						}


						return ASTVisitor.PROCESS_CONTINUE;

					}

					private void visitFunctionCallExpression(
							final AnalysisConfig config,
							final AnalysisResult result, final Checker checker,
							IASTExpression astExpression) {
						IASTExpression functionCallExpression =   ((IASTFunctionCallExpression) astExpression).getFunctionNameExpression();	
						String functionName =functionCallExpression.getRawSignature();
						if(functionCallExpression instanceof IASTIdExpression)
						{
							functionName =((IASTIdExpression) functionCallExpression).getName().toString();
						}

						if(functionName.equals(matchFunction))
						{
							
							IASTInitializerClause[] expParameters =((IASTFunctionCallExpression) astExpression).getArguments();
							for (IASTInitializerClause expParameter : expParameters)
							{
								if(expParameter instanceof IASTLiteralExpression)
								{
									visitLiteralExpressions(config, result,
											checker, functionCallExpression,
											expParameter);
									
								}
								else if(expParameter instanceof IASTIdExpression)
								{
									 visitIdExpressions(
											config, result, checker, functionCallExpression,
											expParameter);
								}
							}

						}
					}

					private void visitLiteralExpressions(
							final AnalysisConfig config,
							final AnalysisResult result, final Checker checker,
							IASTExpression functionCallExpression,
							IASTInitializerClause expParameter) {
						String expArgument =((IASTLiteralExpression) expParameter).toString();										
						for (String paramName : matchString) 
						{
							paramName= "\""+paramName.trim()+"\"";											
							if(expArgument.equals(paramName))
							{
								String description =checker.getDescription().replace("${idName}", paramName);												
								fillDefectData( config, result,checker,functionCallExpression.getFileLocation(),description,expArgument);
							}
						}
					}

					private void visitIdExpressions(
							final AnalysisConfig config,
							final AnalysisResult result, final Checker checker,
							IASTExpression exp,
							IASTInitializerClause expParameter) {
						final IBinding binding = ((IASTIdExpression) expParameter).getName().resolveBinding();
						if ((binding != null) )
						{
							final IASTName[] references = translationUnit.getDeclarationsInAST(binding);	

							for (IASTName reference : references)
							{
								IASTNode node =reference.getParent();

								if(node instanceof IASTDeclarator) 
								{
									IASTInitializer inst =((IASTDeclarator)node).getInitializer();

									if(inst instanceof  CPPASTEqualsInitializer)												
									{																											
										 visitEqualInitializerExpression(
												config, result, checker, exp,
												inst);
									}
									
								}
							}

						}
						
					}

					private void visitEqualInitializerExpression(
							final AnalysisConfig config,
							final AnalysisResult result, final Checker checker,
							IASTExpression exp, IASTInitializer inst) {
						IASTInitializerClause expParameter;
						expParameter = ((CPPASTEqualsInitializer)inst).getInitializerClause();														
						if(expParameter instanceof IASTLiteralExpression)
						{
							String expArgument =expParameter.toString();															
							for (String paramName : matchString) 
							{
								paramName= "\""+paramName.trim()+"\"";
								if(expArgument.equals(paramName))
								{	
									String description =checker.getDescription().replace("${idName}", paramName);
									fillDefectData( config, result,checker,exp.getFileLocation(),description,expArgument);
								}
							}

						}
						
					}						

				};
				visitor.shouldVisitExpressions = true; 					
				ast.accept(visitor);
			}

			private void fillDefectData(AnalysisConfig config,
					AnalysisResult result, Checker checker,
					IASTFileLocation fileLocation, String message, String declaratorName) {
				
				PreOccurence preOcc = createPreOccurence(config, checker, fileLocation, message,declaratorName);
				result.addDefectWithPreOccurence(preOcc);

			}

			private PreOccurence createPreOccurence(AnalysisConfig config,
					Checker checker, IASTFileLocation fileLocation, String msg,String declaratorName) {
				final int startLine = fileLocation.getStartingLineNumber();
				final int endLine = fileLocation.getEndingLineNumber();
				final int startOffset = fileLocation.getNodeOffset();
				final int endOffset = startOffset + fileLocation.getNodeLength();

				Map<String,String> tempmap =CppUtil.extractModuleName(translationUnit, startLine);
				String className =tempmap.get("className");
				String methodName =tempmap.get("methodName");

				PreOccurence preOcc = new PreOccurence();
				preOcc.setCheckerCode(checker.getCode());
				preOcc.setFileName(config.getFileName());
				preOcc.setModulePath(config.getModulePath());
				preOcc.setClassName(className);
				preOcc.setMethodName(methodName);
				preOcc.setLanguage(config.getLanguageEnum().toString());
				preOcc.setSeverityCode(checker.getSeverityCode());
				preOcc.setMessage(checker.getDescription());
				preOcc.setToolName(DexterVdCppPlugin.PLUGIN_NAME);

				preOcc.setStartLine(startLine);
				preOcc.setEndLine(endLine);
				preOcc.setCharStart(startOffset);
				preOcc.setCharEnd(endOffset);
				preOcc.setVariableName(declaratorName);
				preOcc.setStringValue(msg);
				preOcc.setMessage(msg);

				return preOcc;

			}

		};

		return visitor;
	}


}
