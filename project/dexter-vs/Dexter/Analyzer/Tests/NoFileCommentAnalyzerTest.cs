﻿using NUnit.Framework;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CodeFixes;
using Microsoft.CodeAnalysis.Diagnostics;
using System;
using Dexter.Analyzer.Tests.Helpers;
using Dexter.Analyzer;

namespace Dexter.Analyzer.Tests
{
    [TestFixture]
    public class NoFileCommentAnalyzerTest : CodeFixVerifier
    {
        [Test]
        public void NoFileCommentAnalyzer_verfiy_GivenPublicApiFileWithoutDoxygenComment()
        {
            var test = @"using System;
    using System.Collections.Generic;
    using System.Linq;
    using System.Text;
    using System.Threading.Tasks;
    using System.Diagnostics;

    namespace ConsoleApplication1
    {
        public class TestClass
        {   
        }
    }";
            var expected = new DiagnosticResult
            {
                Id = NoFileCommentAnalyzer.DiagnosticId,
                Message = "File 'Test0.cs' has no doxygen comment",
                Severity = DiagnosticSeverity.Warning,
                Locations =
                    new[] {
                            new DiagnosticResultLocation("Test0.cs", 1, 1)
                        }
            };

            VerifyCSharpDiagnostic(test, expected);
        }

        [Test]
        public void NoFileCommentAnalyzer_Ignore_GivenFileWithTestClass()
        {
            var test = @"using System;

    namespace ConsoleApplication1
    {
        [TestFixture]
        public class TestClass
        {   
        }
    }";

            VerifyCSharpDiagnostic(test);
        }

        protected override DiagnosticAnalyzer GetCSharpDiagnosticAnalyzer()
        {
            return new NoFileCommentAnalyzer();
        }
    }


}
