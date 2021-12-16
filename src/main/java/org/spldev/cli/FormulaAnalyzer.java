/* -----------------------------------------------------------------------------
 * Formula-Analysis Lib - Library to analyze propositional formulas.
 * Copyright (C) 2021  Sebastian Krieter
 * 
 * This file is part of Formula-Analysis Lib.
 * 
 * Formula-Analysis Lib is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * Formula-Analysis Lib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Formula-Analysis Lib.  If not, see <https://www.gnu.org/licenses/>.
 * 
 * See <https://github.com/skrieter/formula-analysis> for further information.
 * -----------------------------------------------------------------------------
 */
package org.spldev.cli;

import java.nio.file.*;
import java.util.*;

import org.spldev.cli.analysis.AnalysisAlgorithmManager;
import org.spldev.formula.ModelRepresentation;
import org.spldev.util.cli.AlgorithmWrapper;
import org.spldev.util.cli.CLI;
import org.spldev.util.cli.CLIFunction;
import org.spldev.util.job.*;
import org.spldev.util.logging.*;

/**
 * Command line interface for analyses on feature models.
 *
 * @author Sebastian Krieter
 */
public class FormulaAnalyzer implements CLIFunction {
	private final List<AlgorithmWrapper<org.spldev.formula.analysis.Analysis<?>>> algorithms = AnalysisAlgorithmManager
		.getInstance().getExtensions();

	@Override
	public String getName() {
		return "analyze";
	}

	@Override
	public String getDescription() {
		return "Performs an analysis on a feature model";
	}

	@Override
	public void run(List<String> args) {
		Path fmFile = null;
		AlgorithmWrapper<org.spldev.formula.analysis.Analysis<?>> algorithm = null;
		long timeout = 0;

		final List<String> remainingArguments = new ArrayList<>();
		for (final ListIterator<String> iterator = args.listIterator(); iterator.hasNext();) {
			final String arg = iterator.next();
			switch (arg) {
			case "-a": {
				final String name = CLI.getArgValue(iterator, arg).toLowerCase();
				algorithm = algorithms.stream()
					.filter(a -> Objects.equals(name, a.getName()))
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException("Unknown algorithm: " + name));
				break;
			}
			case "-fm": {
				fmFile = Paths.get(CLI.getArgValue(iterator, arg));
				break;
			}
			case "-t": {
				timeout = Long.parseLong(CLI.getArgValue(iterator, arg));
				break;
			}
			default: {
				remainingArguments.add(arg);
				break;
			}
			}
		}

		if (fmFile == null) {
			throw new IllegalArgumentException("No input file specified!");
		}
		if (algorithm == null) {
			throw new IllegalArgumentException("No algorithm specified!");
		}

		final ModelRepresentation rep = ModelRepresentation.load(fmFile).orElseThrow();
		final org.spldev.formula.analysis.Analysis<?> analysis = algorithm.parseArguments(remainingArguments).orElse(
			Logger::logProblems);

		final long localTime = System.nanoTime();
		final Object result = CLI.runInThread(() -> Executor.run(analysis, rep), timeout).orElse(Logger::logProblems);
		final long timeNeeded = System.nanoTime() - localTime;

		Logger.logInfo("Time:\n" + ((timeNeeded / 1_000_000) / 1000.0) + "s");
		Logger.logInfo("Result:\n" + result);
	}

	@Override
	public String getHelp() {
		final StringBuilder helpBuilder = new StringBuilder();
		helpBuilder.append("\tGeneral Parameters:\n");
		helpBuilder.append("\t\t-fm <Path>   Specify path to feature model file.\n");
		helpBuilder.append("\t\t-a <Name>    Specify algorithm by name. One of:\n");
		algorithms.forEach(a -> helpBuilder.append("\t\t                 ").append(a.getName()).append("\n"));
		helpBuilder.append("\n");
		helpBuilder.append("\tAlgorithm Specific Parameters:\n\t");
		algorithms.forEach(a -> helpBuilder.append(a.getHelp().replace("\n", "\n\t")));
		return helpBuilder.toString();
	}

}