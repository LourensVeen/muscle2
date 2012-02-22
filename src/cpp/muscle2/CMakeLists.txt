cmake_minimum_required(VERSION 2.6)
GET_FILENAME_COMPONENT(PARENT_DIR ${CMAKE_CURRENT_LIST_FILE} PATH)

PROJECT(muscle2 CXX)

FILE( GLOB ALL_SOURCES
	*.h *.hpp *.c *.cpp
	)

# create our library
SET(LIB_NAME ${PROJECT_NAME})
add_library(${LIB_NAME} SHARED ${ALL_SOURCES})


# install
FIND_FILE(INSTALL_DIR build
	${PARENT_DIR}/../../../
)

SET(INSTALL_DIR ${INSTALL_DIR} CACHE STRING "target dir where to put compiled results")
IF(INSTALL_DIR)
	install(
		TARGETS ${LIB_NAME}
		DESTINATION ${INSTALL_DIR}
	)
ENDIF()