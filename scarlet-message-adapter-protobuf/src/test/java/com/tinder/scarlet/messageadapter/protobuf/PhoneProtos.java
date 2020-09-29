/*
 * © 2018 Match Group, LLC.
 */

// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: Phone.proto

package com.tinder.scarlet.messageadapter.protobuf;

public final class PhoneProtos {

    public static final int VOICEMAIL_FIELD_NUMBER = 2;
    /**
     * <code>extend .com.tinder.scarlet.messageadapter.protobuf.Phone { ... }</code>
     */
    public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
            Phone,
            Boolean> voicemail = com.google.protobuf.GeneratedMessage
            .newFileScopedGeneratedExtension(
                    Boolean.class,
                    null);
    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_com_tinder_scarlet_messageadapter_protobuf_Phone_descriptor;
    private static final
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internal_static_com_tinder_scarlet_messageadapter_protobuf_Phone_fieldAccessorTable;
    private static com.google.protobuf.Descriptors.FileDescriptor
            descriptor;

    static {
        String[] descriptorData = {
                "\n\013Phone.proto\022*com.tinder.scarlet.messag" +
                        "eadapter.protobuf\"\035\n\005Phone\022\016\n\006number\030\001 \001" +
                        "(\t*\004\010\002\020\003:D\n\tvoicemail\0221.com.tinder.scarl" +
                        "et.messageadapter.protobuf.Phone\030\002 \001(\010B9" +
                        "\n*com.tinder.scarlet.messageadapter.prot" +
                        "obufB\013PhoneProtos"
        };
        com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
                new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
                    public com.google.protobuf.ExtensionRegistry assignDescriptors(
                            com.google.protobuf.Descriptors.FileDescriptor root) {
                        descriptor = root;
                        return null;
                    }
                };
        com.google.protobuf.Descriptors.FileDescriptor
                .internalBuildGeneratedFileFrom(descriptorData,
                        new com.google.protobuf.Descriptors.FileDescriptor[]{
                        }, assigner);
        internal_static_com_tinder_scarlet_messageadapter_protobuf_Phone_descriptor =
                getDescriptor().getMessageTypes().get(0);
        internal_static_com_tinder_scarlet_messageadapter_protobuf_Phone_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                internal_static_com_tinder_scarlet_messageadapter_protobuf_Phone_descriptor,
                new String[]{"Number",});
        voicemail.internalInit(descriptor.getExtensions().get(0));
    }

    private PhoneProtos() {
    }

    public static void registerAllExtensions(
            com.google.protobuf.ExtensionRegistryLite registry) {
        registry.add(PhoneProtos.voicemail);
    }

    public static void registerAllExtensions(
            com.google.protobuf.ExtensionRegistry registry) {
        registerAllExtensions(
                (com.google.protobuf.ExtensionRegistryLite) registry);
    }

    public static com.google.protobuf.Descriptors.FileDescriptor
    getDescriptor() {
        return descriptor;
    }

    public interface PhoneOrBuilder extends
            // @@protoc_insertion_point(interface_extends:com.tinder.scarlet.messageadapter.protobuf.Phone)
            com.google.protobuf.GeneratedMessageV3.
                    ExtendableMessageOrBuilder<Phone> {

        /**
         * <code>optional string number = 1;</code>
         */
        boolean hasNumber();

        /**
         * <code>optional string number = 1;</code>
         */
        String getNumber();

        /**
         * <code>optional string number = 1;</code>
         */
        com.google.protobuf.ByteString
        getNumberBytes();
    }

    /**
     * Protobuf type {@code com.tinder.scarlet.messageadapter.protobuf.Phone}
     */
    public static final class Phone extends
            com.google.protobuf.GeneratedMessageV3.ExtendableMessage<
                    Phone> implements
            // @@protoc_insertion_point(message_implements:com.tinder.scarlet.messageadapter.protobuf.Phone)
            PhoneOrBuilder {

        public static final int NUMBER_FIELD_NUMBER = 1;
        @Deprecated
        public static final com.google.protobuf.Parser<Phone>
                PARSER = new com.google.protobuf.AbstractParser<Phone>() {
            public Phone parsePartialFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return new Phone(input, extensionRegistry);
            }
        };
        private static final long serialVersionUID = 0L;
        // @@protoc_insertion_point(class_scope:com.tinder.scarlet.messageadapter.protobuf.Phone)
        private static final Phone DEFAULT_INSTANCE;

        static {
            DEFAULT_INSTANCE = new Phone();
        }

        private int bitField0_;
        private volatile Object number_;
        private byte memoizedIsInitialized = -1;

        // Use Phone.newBuilder() to construct.
        private Phone(ExtendableBuilder<Phone, ?> builder) {
            super(builder);
        }

        private Phone() {
            number_ = "";
        }

        private Phone(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            this();
            int mutable_bitField0_ = 0;
            com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                    com.google.protobuf.UnknownFieldSet.newBuilder();
            try {
                boolean done = false;
                while (!done) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            done = true;
                            break;
                        default: {
                            if (!parseUnknownField(input, unknownFields,
                                    extensionRegistry, tag)) {
                                done = true;
                            }
                            break;
                        }
                        case 10: {
                            com.google.protobuf.ByteString bs = input.readBytes();
                            bitField0_ |= 0x00000001;
                            number_ = bs;
                            break;
                        }
                    }
                }
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                throw e.setUnfinishedMessage(this);
            } catch (java.io.IOException e) {
                throw new com.google.protobuf.InvalidProtocolBufferException(
                        e).setUnfinishedMessage(this);
            } finally {
                this.unknownFields = unknownFields.build();
                makeExtensionsImmutable();
            }
        }

        public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
            return PhoneProtos.internal_static_com_tinder_scarlet_messageadapter_protobuf_Phone_descriptor;
        }

        public static Phone parseFrom(
                com.google.protobuf.ByteString data)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static Phone parseFrom(
                com.google.protobuf.ByteString data,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static Phone parseFrom(byte[] data)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static Phone parseFrom(
                byte[] data,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static Phone parseFrom(java.io.InputStream input)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseWithIOException(PARSER, input);
        }

        public static Phone parseFrom(
                java.io.InputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseWithIOException(PARSER, input, extensionRegistry);
        }

        public static Phone parseDelimitedFrom(java.io.InputStream input)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseDelimitedWithIOException(PARSER, input);
        }

        public static Phone parseDelimitedFrom(
                java.io.InputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
        }

        public static Phone parseFrom(
                com.google.protobuf.CodedInputStream input)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseWithIOException(PARSER, input);
        }

        public static Phone parseFrom(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseWithIOException(PARSER, input, extensionRegistry);
        }

        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }

        public static Builder newBuilder(Phone prototype) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
        }

        public static Phone getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static com.google.protobuf.Parser<Phone> parser() {
            return PARSER;
        }

        @Override
        public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
            return this.unknownFields;
        }

        protected FieldAccessorTable
        internalGetFieldAccessorTable() {
            return PhoneProtos.internal_static_com_tinder_scarlet_messageadapter_protobuf_Phone_fieldAccessorTable
                    .ensureFieldAccessorsInitialized(
                            Phone.class, Builder.class);
        }

        /**
         * <code>optional string number = 1;</code>
         */
        public boolean hasNumber() {
            return ((bitField0_ & 0x00000001) == 0x00000001);
        }

        /**
         * <code>optional string number = 1;</code>
         */
        public String getNumber() {
            Object ref = number_;
            if (ref instanceof String) {
                return (String) ref;
            } else {
                com.google.protobuf.ByteString bs =
                        (com.google.protobuf.ByteString) ref;
                String s = bs.toStringUtf8();
                if (bs.isValidUtf8()) {
                    number_ = s;
                }
                return s;
            }
        }

        /**
         * <code>optional string number = 1;</code>
         */
        public com.google.protobuf.ByteString
        getNumberBytes() {
            Object ref = number_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b =
                        com.google.protobuf.ByteString.copyFromUtf8(
                                (String) ref);
                number_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        public final boolean isInitialized() {
            byte isInitialized = memoizedIsInitialized;
            if (isInitialized == 1) {
                return true;
            }
            if (isInitialized == 0) {
                return false;
            }

            if (!extensionsAreInitialized()) {
                memoizedIsInitialized = 0;
                return false;
            }
            memoizedIsInitialized = 1;
            return true;
        }

        public void writeTo(com.google.protobuf.CodedOutputStream output)
                throws java.io.IOException {
            ExtensionWriter
                    extensionWriter = newExtensionWriter();
            if (((bitField0_ & 0x00000001) == 0x00000001)) {
                com.google.protobuf.GeneratedMessageV3.writeString(output, 1, number_);
            }
            extensionWriter.writeUntil(3, output);
            unknownFields.writeTo(output);
        }

        public int getSerializedSize() {
            int size = memoizedSize;
            if (size != -1) {
                return size;
            }

            size = 0;
            if (((bitField0_ & 0x00000001) == 0x00000001)) {
                size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, number_);
            }
            size += extensionsSerializedSize();
            size += unknownFields.getSerializedSize();
            memoizedSize = size;
            return size;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Phone)) {
                return super.equals(obj);
            }
            Phone other = (Phone) obj;

            boolean result = true;
            result = result && (hasNumber() == other.hasNumber());
            if (hasNumber()) {
                result = result && getNumber()
                        .equals(other.getNumber());
            }
            result = result && unknownFields.equals(other.unknownFields);
            result = result &&
                    getExtensionFields().equals(other.getExtensionFields());
            return result;
        }

        @Override
        public int hashCode() {
            if (memoizedHashCode != 0) {
                return memoizedHashCode;
            }
            int hash = 41;
            hash = (19 * hash) + getDescriptorForType().hashCode();
            if (hasNumber()) {
                hash = (37 * hash) + NUMBER_FIELD_NUMBER;
                hash = (53 * hash) + getNumber().hashCode();
            }
            hash = hashFields(hash, getExtensionFields());
            hash = (29 * hash) + unknownFields.hashCode();
            memoizedHashCode = hash;
            return hash;
        }

        public Builder newBuilderForType() {
            return newBuilder();
        }

        public Builder toBuilder() {
            return this == DEFAULT_INSTANCE
                    ? new Builder() : new Builder().mergeFrom(this);
        }

        @Override
        protected Builder newBuilderForType(
                BuilderParent parent) {
            Builder builder = new Builder(parent);
            return builder;
        }

        @Override
        public com.google.protobuf.Parser<Phone> getParserForType() {
            return PARSER;
        }

        public Phone getDefaultInstanceForType() {
            return DEFAULT_INSTANCE;
        }

        /**
         * Protobuf type {@code com.tinder.scarlet.messageadapter.protobuf.Phone}
         */
        public static final class Builder extends
                ExtendableBuilder<
                        Phone, Builder> implements
                // @@protoc_insertion_point(builder_implements:com.tinder.scarlet.messageadapter.protobuf.Phone)
                PhoneOrBuilder {

            private int bitField0_;
            private Object number_ = "";

            // Construct using com.tinder.scarlet.messageadapter.protobuf.PhoneProtos.Phone.newBuilder()
            private Builder() {
                maybeForceBuilderInitialization();
            }

            private Builder(
                    BuilderParent parent) {
                super(parent);
                maybeForceBuilderInitialization();
            }

            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return PhoneProtos.internal_static_com_tinder_scarlet_messageadapter_protobuf_Phone_descriptor;
            }

            protected FieldAccessorTable
            internalGetFieldAccessorTable() {
                return PhoneProtos.internal_static_com_tinder_scarlet_messageadapter_protobuf_Phone_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                Phone.class, Builder.class);
            }

            private void maybeForceBuilderInitialization() {
                if (com.google.protobuf.GeneratedMessageV3
                        .alwaysUseFieldBuilders) {
                }
            }

            public Builder clear() {
                super.clear();
                number_ = "";
                bitField0_ = (bitField0_ & ~0x00000001);
                return this;
            }

            public com.google.protobuf.Descriptors.Descriptor
            getDescriptorForType() {
                return PhoneProtos.internal_static_com_tinder_scarlet_messageadapter_protobuf_Phone_descriptor;
            }

            public Phone getDefaultInstanceForType() {
                return Phone.getDefaultInstance();
            }

            public Phone build() {
                Phone result = buildPartial();
                if (!result.isInitialized()) {
                    throw newUninitializedMessageException(result);
                }
                return result;
            }

            public Phone buildPartial() {
                Phone result = new Phone(this);
                int from_bitField0_ = bitField0_;
                int to_bitField0_ = 0;
                if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
                    to_bitField0_ |= 0x00000001;
                }
                result.number_ = number_;
                result.bitField0_ = to_bitField0_;
                onBuilt();
                return result;
            }

            public Builder clone() {
                return (Builder) super.clone();
            }

            public Builder setField(
                    com.google.protobuf.Descriptors.FieldDescriptor field,
                    Object value) {
                return (Builder) super.setField(field, value);
            }

            public Builder clearField(
                    com.google.protobuf.Descriptors.FieldDescriptor field) {
                return (Builder) super.clearField(field);
            }

            public Builder clearOneof(
                    com.google.protobuf.Descriptors.OneofDescriptor oneof) {
                return (Builder) super.clearOneof(oneof);
            }

            public Builder setRepeatedField(
                    com.google.protobuf.Descriptors.FieldDescriptor field,
                    int index, Object value) {
                return (Builder) super.setRepeatedField(field, index, value);
            }

            public Builder addRepeatedField(
                    com.google.protobuf.Descriptors.FieldDescriptor field,
                    Object value) {
                return (Builder) super.addRepeatedField(field, value);
            }

            public <Type> Builder setExtension(
                    com.google.protobuf.GeneratedMessage.GeneratedExtension<
                            Phone, Type> extension,
                    Type value) {
                return (Builder) super.setExtension(extension, value);
            }

            public <Type> Builder setExtension(
                    com.google.protobuf.GeneratedMessage.GeneratedExtension<
                            Phone, java.util.List<Type>> extension,
                    int index, Type value) {
                return (Builder) super.setExtension(extension, index, value);
            }

            public <Type> Builder addExtension(
                    com.google.protobuf.GeneratedMessage.GeneratedExtension<
                            Phone, java.util.List<Type>> extension,
                    Type value) {
                return (Builder) super.addExtension(extension, value);
            }

            public <Type> Builder clearExtension(
                    com.google.protobuf.GeneratedMessage.GeneratedExtension<
                            Phone, ?> extension) {
                return (Builder) super.clearExtension(extension);
            }

            public Builder mergeFrom(com.google.protobuf.Message other) {
                if (other instanceof Phone) {
                    return mergeFrom((Phone) other);
                } else {
                    super.mergeFrom(other);
                    return this;
                }
            }

            public Builder mergeFrom(Phone other) {
                if (other == Phone.getDefaultInstance()) {
                    return this;
                }
                if (other.hasNumber()) {
                    bitField0_ |= 0x00000001;
                    number_ = other.number_;
                    onChanged();
                }
                this.mergeExtensionFields(other);
                this.mergeUnknownFields(other.unknownFields);
                onChanged();
                return this;
            }

            public final boolean isInitialized() {
                if (!extensionsAreInitialized()) {
                    return false;
                }
                return true;
            }

            public Builder mergeFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                Phone parsedMessage = null;
                try {
                    parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    parsedMessage = (Phone) e.getUnfinishedMessage();
                    throw e.unwrapIOException();
                } finally {
                    if (parsedMessage != null) {
                        mergeFrom(parsedMessage);
                    }
                }
                return this;
            }

            /**
             * <code>optional string number = 1;</code>
             */
            public boolean hasNumber() {
                return ((bitField0_ & 0x00000001) == 0x00000001);
            }

            /**
             * <code>optional string number = 1;</code>
             */
            public String getNumber() {
                Object ref = number_;
                if (!(ref instanceof String)) {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    String s = bs.toStringUtf8();
                    if (bs.isValidUtf8()) {
                        number_ = s;
                    }
                    return s;
                } else {
                    return (String) ref;
                }
            }

            /**
             * <code>optional string number = 1;</code>
             */
            public Builder setNumber(
                    String value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                bitField0_ |= 0x00000001;
                number_ = value;
                onChanged();
                return this;
            }

            /**
             * <code>optional string number = 1;</code>
             */
            public com.google.protobuf.ByteString
            getNumberBytes() {
                Object ref = number_;
                if (ref instanceof String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (String) ref);
                    number_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }

            /**
             * <code>optional string number = 1;</code>
             */
            public Builder setNumberBytes(
                    com.google.protobuf.ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                bitField0_ |= 0x00000001;
                number_ = value;
                onChanged();
                return this;
            }

            /**
             * <code>optional string number = 1;</code>
             */
            public Builder clearNumber() {
                bitField0_ = (bitField0_ & ~0x00000001);
                number_ = getDefaultInstance().getNumber();
                onChanged();
                return this;
            }

            public final Builder setUnknownFields(
                    final com.google.protobuf.UnknownFieldSet unknownFields) {
                return super.setUnknownFields(unknownFields);
            }

            public final Builder mergeUnknownFields(
                    final com.google.protobuf.UnknownFieldSet unknownFields) {
                return super.mergeUnknownFields(unknownFields);
            }

            // @@protoc_insertion_point(builder_scope:com.tinder.scarlet.messageadapter.protobuf.Phone)
        }

    }

    // @@protoc_insertion_point(outer_class_scope)
}
