#include <iostream>
#include <jni.h>
#include <vata/explicit_tree_aut.hh>
#include <vata/parsing/timbuk_parser.hh>
#include <vata/serialization/timbuk_serializer.hh>
#include "common_VATACommands.hpp"

using VATA::AutBase;
using VATA::ExplicitTreeAut;
using VATA::InclParam;
using VATA::Parsing::AbstrParser;
using VATA::Parsing::TimbukParser;
using VATA::Serialization::AbstrSerializer;
using VATA::Serialization::TimbukSerializer;

JNIEXPORT jstring JNICALL Java_common_VATACommands_reduce
  (JNIEnv *env, jclass theClass, jstring targetInput)
{
    // load target
    std::unique_ptr<AbstrParser> parser(new TimbukParser());
    std::unique_ptr<AbstrSerializer> serializer(new TimbukSerializer());
    AutBase::StateDict stateDict;
    ExplicitTreeAut target;

    const char *targetInputString = env->GetStringUTFChars(targetInput, NULL);
    target.LoadFromString(*parser, targetInputString, stateDict);
    env->ReleaseStringUTFChars(targetInput, targetInputString);

    // reduce target
    return env->NewStringUTF(target.Reduce().DumpToString(*serializer, stateDict).c_str());
}

JNIEXPORT jboolean JNICALL Java_common_VATACommands_checkInclusion
  (JNIEnv *env, jclass theClass, jstring subsumerInput, jstring includerInput)
{
    // load subsumer
    std::unique_ptr<AbstrParser> parserSub(new TimbukParser());
    std::unique_ptr<AbstrSerializer> serializerSub(new TimbukSerializer());
    AutBase::StateDict stateDictSub;
    ExplicitTreeAut subsumer;

    const char *subsumerInputString = env->GetStringUTFChars(subsumerInput, NULL);
    subsumer.LoadFromString(*parserSub, subsumerInputString, stateDictSub);
    env->ReleaseStringUTFChars(subsumerInput, subsumerInputString);

    // load includer
    std::unique_ptr<AbstrParser> parserIncl(new TimbukParser());
    std::unique_ptr<AbstrSerializer> serializerIncl(new TimbukSerializer());
    AutBase::StateDict stateDictIncl;
    ExplicitTreeAut includer;

    const char *includerInputString = env->GetStringUTFChars(includerInput, NULL);
    includer.LoadFromString(*parserIncl, includerInputString, stateDictIncl);
    env->ReleaseStringUTFChars(includerInput, includerInputString);

    // prepare inclusion check (with default settings)
    InclParam ip;
    ip.SetAlgorithm(InclParam::e_algorithm::antichains);
    ip.SetDirection(InclParam::e_direction::upward);
    ip.SetUseRecursion(false);
    ip.SetUseDownwardCacheImpl(false);
    ip.SetSearchOrder(InclParam::e_search_order::depth);

    return ExplicitTreeAut::CheckInclusion(subsumer, includer, ip);
}
