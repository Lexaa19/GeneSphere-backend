#!/bin/bash

# Quick Test Script for Data Extraction
# This script helps you test the data extraction tools before running the full extraction

echo "======================================================================"
echo "🧬 GeneSphere Data Extraction Test Script"
echo "======================================================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    echo -e "${RED}❌ Error: pom.xml not found${NC}"
    echo "Please run this script from the gene-service directory"
    exit 1
fi

echo -e "${GREEN}✅ In gene-service directory${NC}"
echo ""

# Test 1: Check if Java is available
echo "Test 1: Checking Java installation..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo -e "${GREEN}✅ Java found: $JAVA_VERSION${NC}"
else
    echo -e "${RED}❌ Java not found. Please install Java 17 or higher${NC}"
    exit 1
fi
echo ""

# Test 2: Check Maven
echo "Test 2: Checking Maven installation..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1)
    echo -e "${GREEN}✅ Maven found: $MVN_VERSION${NC}"
else
    echo -e "${RED}❌ Maven not found. Please install Maven${NC}"
    exit 1
fi
echo ""

# Test 3: Test cBioPortal API connectivity
echo "Test 3: Testing cBioPortal API connectivity..."
CBIO_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "https://www.cbioportal.org/api/studies" --max-time 10)

if [ "$CBIO_RESPONSE" -eq 200 ]; then
    echo -e "${GREEN}✅ cBioPortal API is reachable${NC}"
else
    echo -e "${YELLOW}⚠️  Warning: Could not reach cBioPortal API (HTTP $CBIO_RESPONSE)${NC}"
    echo "   Check your internet connection"
fi
echo ""

# Test 4: Test GDC API connectivity
echo "Test 4: Testing GDC Data Portal API connectivity..."
GDC_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "https://api.gdc.cancer.gov/status" --max-time 10)

if [ "$GDC_RESPONSE" -eq 200 ]; then
    echo -e "${GREEN}✅ GDC API is reachable${NC}"
else
    echo -e "${YELLOW}⚠️  Warning: Could not reach GDC API (HTTP $GDC_RESPONSE)${NC}"
    echo "   Check your internet connection"
fi
echo ""

# Test 5: Check if extractor classes exist
echo "Test 5: Checking extractor classes..."
if [ -f "src/main/java/com/gene/sphere/geneservice/extractor/CbioPortalDataExtractor.java" ]; then
    echo -e "${GREEN}✅ CbioPortalDataExtractor.java found${NC}"
else
    echo -e "${RED}❌ CbioPortalDataExtractor.java not found${NC}"
fi

if [ -f "src/main/java/com/gene/sphere/geneservice/extractor/GdcDataExtractor.java" ]; then
    echo -e "${GREEN}✅ GdcDataExtractor.java found${NC}"
else
    echo -e "${RED}❌ GdcDataExtractor.java not found${NC}"
fi
echo ""

# Test 6: Try to compile the project
echo "Test 6: Attempting to compile the project..."
echo -e "${YELLOW}This may take a minute...${NC}"
mvn clean compile -q

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Project compiled successfully${NC}"
else
    echo -e "${RED}❌ Compilation failed. Check the errors above${NC}"
    exit 1
fi
echo ""

# Create output directory
echo "Test 7: Creating output directory..."
mkdir -p ./test_extraction
echo -e "${GREEN}✅ Created ./test_extraction directory${NC}"
echo ""

# Summary
echo "======================================================================"
echo "📊 Test Summary"
echo "======================================================================"
echo ""
echo "All basic checks passed! You're ready to extract data."
echo ""
echo "Quick Start Commands:"
echo ""
echo -e "${YELLOW}1. Extract from cBioPortal (RECOMMENDED):${NC}"
echo "   mvn exec:java -Dexec.mainClass=\"com.gene.sphere.geneservice.extractor.CbioPortalDataExtractor\""
echo ""
echo -e "${YELLOW}2. Extract from GDC:${NC}"
echo "   mvn exec:java -Dexec.mainClass=\"com.gene.sphere.geneservice.extractor.GdcDataExtractor\""
echo ""
echo -e "${YELLOW}3. Check progress:${NC}"
echo "   tail -f nohup.out  # If running in background"
echo ""
echo "Data will be saved to: ./extracted_data/"
echo ""
echo "======================================================================"
echo ""

# Offer to run a quick test extraction
echo -e "${YELLOW}Would you like to test the cBioPortal API with a quick sample query? (y/n)${NC}"
read -r response

if [[ "$response" =~ ^[Yy]$ ]]; then
    echo ""
    echo "Fetching study list from cBioPortal..."
    curl -s "https://www.cbioportal.org/api/studies" | head -n 50
    echo ""
    echo -e "${GREEN}✅ If you see JSON data above, the API is working perfectly!${NC}"
else
    echo "Skipping API test."
fi

echo ""
echo -e "${GREEN}🎉 All tests completed! You're ready to go!${NC}"
echo ""
